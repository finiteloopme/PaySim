package org.paysim.paysim.parameters;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.paysim.paysim.output.Output;

public class Parameters {
    private String seedString;
    public int nbClients, nbMerchants, nbBanks, nbFraudsters, nbSteps;
    public  double multiplier, fraudProbability, transferLimit;
    public  String aggregatedTransactions, maxOccurrencesPerClient, initialBalancesDistribution,
            overdraftLimits, clientsProfilesFile, transactionsTypes;
    public  String typologiesFolder, outputPath;
    public  boolean saveToDB;
    public  String dbUrl, dbUser, dbPassword;

    public  StepsProfiles stepsProfiles;
    public  ClientsProfiles clientsProfiles;

    private final String propFile = "PaySim.properties";
    private static Parameters parameters = new Parameters();

    private Parameters(){
        initParameters(propFile);
    }

    public static Parameters getInstance(){
        return parameters;
    }

    public  void initParameters(String propertiesFile) {
        loadPropertiesFile(propertiesFile);

        ActionTypes.loadActionTypes(transactionsTypes);
        BalancesClients.initBalanceClients(initialBalancesDistribution);
        BalancesClients.initOverdraftLimits(overdraftLimits);
        clientsProfiles = new ClientsProfiles(clientsProfilesFile);
        stepsProfiles = new StepsProfiles(aggregatedTransactions, multiplier, nbSteps);
        ActionTypes.loadMaxOccurrencesPerClient(maxOccurrencesPerClient);
    }

    private  void loadPropertiesFile(String propertiesFile) {
        try {
            Properties parameters = new Properties();
            //parameters.load(new FileInputStream("classpath:/" + propertiesFile));
            parameters.load(this.getClass().getResourceAsStream("/BOOT-INF/classes/src/main/resources/" +propertiesFile));
            seedString = String.valueOf(parameters.getProperty("seed"));
            nbSteps = Integer.parseInt(parameters.getProperty("nbSteps"));
            multiplier = Double.parseDouble(parameters.getProperty("multiplier"));

            nbClients = Integer.parseInt(parameters.getProperty("nbClients"));
            nbFraudsters = Integer.parseInt(parameters.getProperty("nbFraudsters"));
            nbMerchants = Integer.parseInt(parameters.getProperty("nbMerchants"));
            nbBanks = Integer.parseInt(parameters.getProperty("nbBanks"));

            fraudProbability = Double.parseDouble(parameters.getProperty("fraudProbability"));
            transferLimit = Double.parseDouble(parameters.getProperty("transferLimit"));

            transactionsTypes = parameters.getProperty("transactionsTypes");
            aggregatedTransactions = parameters.getProperty("aggregatedTransactions");
            maxOccurrencesPerClient = parameters.getProperty("maxOccurrencesPerClient");
            initialBalancesDistribution = parameters.getProperty("initialBalancesDistribution");
            overdraftLimits = parameters.getProperty("overdraftLimits");
            clientsProfilesFile = parameters.getProperty("clientsProfiles");

            typologiesFolder = parameters.getProperty("typologiesFolder");
            outputPath = parameters.getProperty("outputPath");

            saveToDB = parameters.getProperty("saveToDB").equals("1");
            dbUrl = parameters.getProperty("dbUrl");
            dbUser = parameters.getProperty("dbUser");
            dbPassword = parameters.getProperty("dbPassword");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public  int getSeed() {
        // /!\ MASON seed is using an int internally
        // https://github.com/eclab/mason/blob/66d38fa58fae3e250b89cf6f31bcfa9d124ffd41/mason/sim/engine/SimState.java#L45
        if (seedString.equals("time")) {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        } else {
            return Integer.parseInt(seedString);
        }
    }

    public  String toString(long seed) {
        ArrayList<String> properties = new ArrayList<>();

        properties.add("seed=" + seed);
        properties.add("nbSteps=" + nbSteps);
        properties.add("multiplier=" + multiplier);
        properties.add("nbFraudsters=" + nbFraudsters);
        properties.add("nbMerchants=" + nbMerchants);
        properties.add("fraudProbability=" + fraudProbability);
        properties.add("transferLimit=" + transferLimit);
        properties.add("transactionsTypes=" + transactionsTypes);
        properties.add("aggregatedTransactions=" + aggregatedTransactions);
        properties.add("clientsProfilesFile=" + clientsProfilesFile);
        properties.add("initialBalancesDistribution=" + initialBalancesDistribution);
        properties.add("maxOccurrencesPerClient=" + maxOccurrencesPerClient);
        properties.add("outputPath=" + outputPath);
        properties.add("saveToDB=" + saveToDB);
        properties.add("dbUrl=" + dbUrl);
        properties.add("dbUser=" + dbUser);
        properties.add("dbPassword=" + dbPassword);

        return String.join(Output.EOL_CHAR, properties);
    }
}
