package org.paysim.paysim;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import sim.engine.SimState;

import org.paysim.paysim.parameters.*;

import org.paysim.paysim.actors.Bank;
import org.paysim.paysim.actors.Client;
import org.paysim.paysim.actors.Fraudster;
import org.paysim.paysim.actors.Merchant;
import org.paysim.paysim.actors.networkdrugs.NetworkDrug;

import org.paysim.paysim.base.Transaction;
import org.paysim.paysim.base.ClientActionProfile;
import org.paysim.paysim.base.StepActionProfile;

import org.paysim.paysim.output.Output;

//SpringBoot Framework
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PaySim extends SimState {
    public static final double PAYSIM_VERSION = 2.0;
    private static final String[] DEFAULT_ARGS = new String[]{"", "-file", "PaySim.properties", "5"};

    public final String simulationName;
    private int totalTransactionsMade = 0;
    private int stepParticipated = 0;

    private ArrayList<Client> clients = new ArrayList<>();
    private ArrayList<Merchant> merchants = new ArrayList<>();
    private ArrayList<Fraudster> fraudsters = new ArrayList<>();
    private ArrayList<Bank> banks = new ArrayList<>();

    private ArrayList<Transaction> transactions = new ArrayList<>();
    private int currentStep;

    private Map<ClientActionProfile, Integer> countProfileAssignment = new HashMap<>();
    private static Parameters params = null;

    public static void main(String[] args) {
        
        String _args[] = DEFAULT_ARGS;
        String propertiesFile = "";
        for (int x = 0; x < _args.length - 1; x++) {
            if (_args[x].equals("-file")) {
                propertiesFile = _args[x + 1];
            }
        }
        params = Parameters.getInstance();
        params.initParameters(propertiesFile);
        SpringApplication.run(PaySim.class, _args);
    }

    @RestController
    class SimulationController {
        @GetMapping("/")
        String startSimulation() {
            System.out.println("PAYSIM: Financial Simulator v" + PAYSIM_VERSION);
            String args[] = DEFAULT_ARGS;
            int nbTimesRepeat = Integer.parseInt(args[3]);
            for (int i = 0; i < nbTimesRepeat; i++) {
                PaySim p = new PaySim();
                p.runSimulation();
            }
            return "Started the simulation!";
        }
    }


    public PaySim() {
        super(params.getSeed());
        BalancesClients.setRandom(random);
        params.clientsProfiles.setRandom(random);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date currentTime = new Date();
        simulationName = "PS_" + dateFormat.format(currentTime) + "_" + seed();

        File simulationFolder = new File(params.outputPath + simulationName);
        simulationFolder.mkdirs();

        Output.initOutputFilenames(simulationName);
        Output.writeParameters(seed());
    }

    private void runSimulation() {
        System.out.println();
        System.out.println("Starting PaySim Running for " + params.nbSteps + " steps.");
        long startTime = System.currentTimeMillis();
        super.start();

        initCounters();
        initActors();

        while ((currentStep = (int) schedule.getSteps()) < params.nbSteps) {
            if (!schedule.step(this))
                break;

            writeOutputStep();
            if (currentStep % 100 == 100 - 1) {
                System.out.println("Step " + currentStep);
            } else {
                System.out.print("*");
            }
        }
        System.out.println();
        System.out.println("Finished running " + currentStep + " steps ");
        finish();

        double total = System.currentTimeMillis() - startTime;
        total = total / 1000 / 60;
        System.out.println("It took: " + total + " minutes to execute the simulation");
        System.out.println("Simulation name: " + simulationName);
        System.out.println();
    }

    private void initCounters() {
        for (String action : ActionTypes.getActions()) {
            for (ClientActionProfile clientActionProfile : params.clientsProfiles.getProfilesFromAction(action)) {
                countProfileAssignment.put(clientActionProfile, 0);
            }
        }
    }

    private void initActors() {
        System.out.println("Init - Seed " + seed());

        //Add the merchants
        System.out.println("NbMerchants: " + (int) (params.nbMerchants * params.multiplier));
        for (int i = 0; i < params.nbMerchants * params.multiplier; i++) {
            Merchant m = new Merchant(generateId());
            merchants.add(m);
        }

        //Add the fraudsters
        System.out.println("NbFraudsters: " + (int) (params.nbFraudsters * params.multiplier));
        for (int i = 0; i < params.nbFraudsters * params.multiplier; i++) {
            Fraudster f = new Fraudster(generateId());
            fraudsters.add(f);
            schedule.scheduleRepeating(f);
        }

        //Add the banks
        System.out.println("NbBanks: " + params.nbBanks);
        for (int i = 0; i < params.nbBanks; i++) {
            Bank b = new Bank(generateId());
            banks.add(b);
        }

        //Add the clients
        System.out.println("NbClients: " + (int) (params.nbClients * params.multiplier));
        for (int i = 0; i < params.nbClients * params.multiplier; i++) {
            Client c = new Client(this);
            clients.add(c);
        }

        NetworkDrug.createNetwork(this, params.typologiesFolder + TypologiesFiles.drugNetworkOne);

        // Do not write code under this part otherwise clients will not be used in simulation
        // Schedule clients to act at each step of the simulation
        for (Client c : clients) {
            schedule.scheduleRepeating(c);
        }
    }

    public Map<String, ClientActionProfile> pickNextClientProfile() {
        Map<String, ClientActionProfile> profile = new HashMap<>();
        for (String action : ActionTypes.getActions()) {
            ClientActionProfile clientActionProfile = params.clientsProfiles.pickNextActionProfile(action);

            profile.put(action, clientActionProfile);

            int count = countProfileAssignment.get(clientActionProfile);
            countProfileAssignment.put(clientActionProfile, count + 1);
        }
        return profile;
    }

    public void finish() {
        Output.writeFraudsters(fraudsters);
        Output.writeClientsProfiles(countProfileAssignment, (int) (params.nbClients * params.multiplier));
        Output.writeSummarySimulation(this);
    }

    private void resetVariables() {
        if (transactions.size() > 0) {
            stepParticipated++;
        }
        transactions = new ArrayList<>();
    }

    private void writeOutputStep() {
        ArrayList<Transaction> transactions = getTransactions();

        totalTransactionsMade += transactions.size();

        Output.incrementalWriteRawLog(currentStep, transactions);
        if (params.saveToDB) {
            Output.writeDatabaseLog(params.dbUrl, params.dbUser, params.dbPassword, transactions, simulationName);
        }

        Output.incrementalWriteStepAggregate(currentStep, transactions);
        resetVariables();
    }

    public String generateId() {
        final String alphabet = "0123456789";
        final int sizeId = 10;
        StringBuilder idBuilder = new StringBuilder(sizeId);

        for (int i = 0; i < sizeId; i++)
            idBuilder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return idBuilder.toString();
    }

    public Merchant pickRandomMerchant() {
        return merchants.get(random.nextInt(merchants.size()));
    }

    public Bank pickRandomBank() {
        return banks.get(random.nextInt(banks.size()));
    }

    public Client pickRandomClient(String nameOrig) {
        Client clientDest = null;

        String nameDest = nameOrig;
        while (nameOrig.equals(nameDest)) {
            clientDest = clients.get(random.nextInt(clients.size()));
            nameDest = clientDest.getName();
        }
        return clientDest;
    }

    public int getTotalTransactions() {
        return totalTransactionsMade;
    }

    public int getStepParticipated() {
        return stepParticipated;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public void addClient(Client c) {
        clients.add(c);
    }

    public int getStepTargetCount() {
        return params.stepsProfiles.getTargetCount(currentStep);
    }

    public Map<String, Double> getStepProbabilities() {
        return params.stepsProfiles.getProbabilitiesPerStep(currentStep);
    }

    public StepActionProfile getStepAction(String action) {
        return params.stepsProfiles.getActionForStep(currentStep, action);
    }
}