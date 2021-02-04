package org.paysim.paysim.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.paysim.paysim.PaySim;
import org.paysim.paysim.base.StepActionProfile;
import org.paysim.paysim.base.ClientActionProfile;
import org.paysim.paysim.base.Transaction;
import org.paysim.paysim.actors.Fraudster;
import org.paysim.paysim.parameters.Parameters;
import org.paysim.paysim.parameters.StepsProfiles;
import org.paysim.paysim.utils.DatabaseHandler;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

public class Output {
    public static final int PRECISION_OUTPUT = 2;
    public static final String OUTPUT_SEPARATOR = ",", EOL_CHAR = System.lineSeparator();
    private static String filenameGlobalSummary, filenameParameters, filenameSummary, filenameRawLog,
            filenameStepAggregate, filenameClientProfiles, filenameFraudsters;
    private static Parameters params = Parameters.getInstance();
    public static void incrementalWriteRawLog(int step, ArrayList<Transaction> transactions) {
        String rawLogHeader = "step,action,amount,nameOrig,oldBalanceOrig,newBalanceOrig,nameDest,oldBalanceDest,newBalanceDest,isFraud,isFlaggedFraud,isUnauthorizedOverdraft";
        try {
            //BufferedWriter writer = new BufferedWriter(new FileWriter(filenameRawLog, true));
            if (step == 0) {
                publishToPubsub(rawLogHeader);
                //writer.write(rawLogHeader);
                //writer.newLine();
            }
            for (Transaction t : transactions) {
                publishToPubsub(t.toString());
                //writer.write(t.toString());
                //writer.newLine();
            }
            //writer.close();
        // } catch (IOException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void publishToPubsub(String rawTransaction) throws IOException, InterruptedException{
        //TODO: fix the hardcoding
        String projectId = "kl-dev-scratchpad";
        String topicId = "raw-fin-transactions";

        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();
      
            //List<String> messages = Arrays.asList("first message", "second message");
      
            //for (final String message : messages) {
            //   ByteString data = ByteString.copyFromUtf8(message);
              ByteString data = ByteString.copyFromUtf8(rawTransaction);
              PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
      
              // Once published, returns a server-assigned message id (unique within the topic)
              ApiFuture<String> future = publisher.publish(pubsubMessage);
      
              // Add an asynchronous callback to handle success / failure
              ApiFutures.addCallback(
                  future,
                  new ApiFutureCallback<String>() {
      
                    @Override
                    public void onFailure(Throwable throwable) {
                      if (throwable instanceof ApiException) {
                        ApiException apiException = ((ApiException) throwable);
                        // details on the API exception
                        System.out.println(apiException.getStatusCode().getCode());
                        System.out.println(apiException.isRetryable());
                      }
                      //System.out.println("Error publishing message : " + message);
                      System.out.println("Error publishing message : " + rawTransaction);
                    }
      
                    @Override
                    public void onSuccess(String messageId) {
                      // Once published, returns server-assigned message ids (unique within the topic)
                      System.out.println("Published message ID: " + messageId);
                    }
                  },
                  MoreExecutors.directExecutor());
            //}
        } finally {
            if (publisher != null) {
              // When finished with the publisher, shutdown to free up resources.
              publisher.shutdown();
              publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }

    }

    public static void incrementalWriteStepAggregate(int step, ArrayList<Transaction> transactions) {
        String stepAggregateHeader = "action,month,day,hour,count,sum,avg,std,step";
        Map<String, StepActionProfile> stepRecord = Aggregator.generateStepAggregate(step, transactions);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameStepAggregate, true));
            if (step == 0) {
                writer.write(stepAggregateHeader);
                writer.newLine();
            }
            for (StepActionProfile actionRecord : stepRecord.values()) {
                writer.write(actionRecord.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void writeFraudsters(ArrayList<Fraudster> fraudsters) {
        String fraudsterHeader = "name,nbVictims,profit";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameFraudsters));
            writer.write(fraudsterHeader);
            writer.newLine();
            for (Fraudster f : fraudsters) {
                writer.write(f.toString());
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeParameters(long seed) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameParameters));
            writer.write(params.toString(seed));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeClientsProfiles(Map<ClientActionProfile, Integer> countPerClientActionProfile, int numberClients) {
        String clientsProfilesHeader = "action,high,low,total,freq";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameClientProfiles));
            writer.write(clientsProfilesHeader);
            writer.newLine();

            for (Map.Entry<ClientActionProfile, Integer> counterActionProfile : countPerClientActionProfile.entrySet()) {
                ClientActionProfile clientActionProfile = counterActionProfile.getKey();
                String action = clientActionProfile.getAction();
                int count = counterActionProfile.getValue();

                double probability = ((double) count) / numberClients;

                writer.write(action + "," + clientActionProfile.getMinCount() + "," + clientActionProfile.getMaxCount() + ","
                        + count + "," + fastFormatDouble(5, probability));
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeSummarySimulation(PaySim paySim) {
        StringBuilder errorSummary = new StringBuilder();
        StepsProfiles simulationStepsProfiles = new StepsProfiles(Output.filenameStepAggregate, 1 / params.multiplier, params.nbSteps);
        double totalErrorRate = SummaryBuilder.buildSummary(params.stepsProfiles, simulationStepsProfiles, errorSummary);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Output.filenameSummary));
            writer.write(errorSummary.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String summary = paySim.simulationName + "," + params.nbSteps + "," + paySim.getTotalTransactions() + "," +
                paySim.getClients().size() + "," + totalErrorRate;
        writeGlobalSummary(summary);

        System.out.println("Nb of clients: " + paySim.getClients().size() + " - Nb of steps with transactions: " + paySim.getStepParticipated());
    }

    private static void writeGlobalSummary(String summary) {
        String header = "name,steps,nbTransactions,nbClients,totalError";
        File f = new File(filenameGlobalSummary);
        boolean fileExists = f.exists();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f, true));
            if (!fileExists) {
                writer.write(header);
                writer.newLine();
            }
            writer.write(summary);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeDatabaseLog(String dbUrl, String dbUser, String dbPassword,
                                        ArrayList<Transaction> transactions, String simulatorName) {
        DatabaseHandler handler = new DatabaseHandler(dbUrl, dbUser, dbPassword);
        for (Transaction t : transactions) {
            handler.insert(simulatorName, t);
        }
    }

    //See https://stackoverflow.com/a/10554128
    private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000};

    public static String fastFormatDouble(int precision, double val) {
        StringBuilder sb = new StringBuilder();
        if (val < 0) {
            sb.append('-');
            val = -val;
        }
        int exp = POW10[precision];
        long lval = (long) (val * exp + 0.5);
        sb.append(lval / exp).append('.');
        long fval = lval % exp;
        for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
            sb.append('0');
        }
        sb.append(fval);
        return sb.toString();
    }

    public static String formatBoolean(boolean bool) {
        return bool ? "1" : "0";
    }

    public static void initOutputFilenames(String simulatorName) {
        String outputBaseString = params.outputPath + simulatorName + "//" + simulatorName;
        filenameGlobalSummary = params.outputPath + "summary.csv";

        filenameParameters = outputBaseString + "_PaySim.properties";
        filenameSummary = outputBaseString + "_Summary.txt";

        filenameRawLog = outputBaseString + "_rawLog.csv";
        filenameStepAggregate = outputBaseString + "_aggregatedTransactions.csv";
        filenameClientProfiles = outputBaseString + "_clientsProfiles.csv";
        filenameFraudsters = outputBaseString + "_fraudsters.csv";
    }
}
