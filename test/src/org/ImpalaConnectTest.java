package org.ImpalaConnectTest;

import java.util.List;
import java.util.UUID;

import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
import org.apache.hive.service.cli.thrift.*;

import com.cloudera.impala.thrift.*;
import com.cloudera.beeswax.api.*;

public class ImpalaConnectTest
{
    private static String host="localhost";
    private static String stmt="SHOW TABLES";

    public static void main(String [] args) 
    {
        if (args.length < 2) {
            System.out.println("Usage: ImpalaConnectTest host query");
            return;
        }
        
        try {
            host = args[0];
            stmt = args[1];

            System.out.println("host: " + host + "");
            System.out.println("stmt: " + stmt + "");

            int hive2Port = 21050;
            int beeswaxPort = 21000;

            System.out.println("Testing with Hive2 on port: " + hive2Port + "");
            ImpalaConnectTest.testConnectionHiveServer2(host, hive2Port, stmt);
            System.out.println("---");

            System.out.println("Testing with Beeswax on port: " + beeswaxPort + "");
            ImpalaConnectTest.testConnectionBeeswax(host, beeswaxPort, stmt);
            System.out.println("---");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected static void testConnectionBeeswax(String host, int port, String statement){
        try {
            //open connection
            TSocket transport = new TSocket(host,port);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);

            //connect to client
            ImpalaService$Client client = new ImpalaService.Client(protocol);
            client.PingImpalaService();
            
            Query query = new Query();
            query.setQuery(statement); // hive statement: SELECT * FROM table LIMIT 10;
            
            QueryHandle handle = client.query(query);
            
            boolean done = false;
            while(done == false) {
                Results results = client.fetch(handle,false,100);
                QueryState queryState = client.get_state(handle);
                /*
                while(queryState != ImpalaService$Client.FINISHED) {
                    //sleep(0.5)
                    queryState = client.get_state(query);
                }
                */
            
                List<String> data = results.data;
            
                for(int i=0;i<data.size();i++) {
                    System.out.println(data.get(i));
                }

                if(results.has_more==false) {
                    done = true;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    protected static void testConnectionHiveServer2(String host, int port, String statement) {
        try {
            TSocket transport = new TSocket(host,port);
            
            transport.setTimeout(60000);
            TBinaryProtocol protocol = new TBinaryProtocol(transport);
            ImpalaHiveServer2Service.Client client = new ImpalaHiveServer2Service.Client(protocol);  
            
            transport.open();
            
            String username = "";
            String password = "";

            TOpenSessionReq openReq = new TOpenSessionReq();
            openReq.setClient_protocol(TProtocolVersion.HIVE_CLI_SERVICE_PROTOCOL_V1);
            openReq.setUsername(username);
            openReq.setPassword(password);

            TOpenSessionResp openResp = client.OpenSession(openReq);
            org.apache.hive.service.cli.thrift.TStatus status = openResp.getStatus();
            if (status.getStatusCode() == org.apache.hive.service.cli.thrift.TStatusCode.ERROR_STATUS) {
                String msg = status.getErrorMessage();
                System.out.println(msg);
                return;
            }
            if(status.getStatusCode() != org.apache.hive.service.cli.thrift.TStatusCode.SUCCESS_STATUS) {
                System.out.println("No success");
                return;
            }
            TSessionHandle sessHandle = openResp.getSessionHandle();
            
            TExecuteStatementReq execReq = new TExecuteStatementReq(sessHandle, statement);
            TExecuteStatementResp execResp = client.ExecuteStatement(execReq);
            status = execResp.getStatus();
            if (status.getStatusCode() == org.apache.hive.service.cli.thrift.TStatusCode.ERROR_STATUS) {
                String msg = status.getErrorMessage();
                System.out.println(msg + "," + status.getSqlState() + "," + Integer.toString(status.getErrorCode()) + "," + status.isSetInfoMessages());
                System.out.println("After ExecuteStatement: " + statement);
                return;
            }

            TOperationHandle stmtHandle = execResp.getOperationHandle();

            if (stmtHandle == null) {
                System.out.println("Empty operation handle");
                return;
            }

            TFetchResultsReq fetchReq = new TFetchResultsReq();
            fetchReq.setOperationHandle(stmtHandle);
            fetchReq.setMaxRows(100);
            //org.apache.hive.service.cli.thrift.TFetchOrientation.FETCH_NEXT
            TFetchResultsResp resultsResp = client.FetchResults(fetchReq);

            status = resultsResp.getStatus();
            if (status.getStatusCode() == org.apache.hive.service.cli.thrift.TStatusCode.ERROR_STATUS) {
                String msg = status.getErrorMessage();
                System.out.println(msg + "," + status.getSqlState() + "," + Integer.toString(status.getErrorCode()) + "," + status.isSetInfoMessages());
                System.out.println("After FetchResults: " + statement);
                return;
            }

            TRowSet resultsSet = resultsResp.getResults();
            List<TRow> resultRows = resultsSet.getRows();
            System.out.println("Result size = " + Integer.toString(resultRows.size()) );
            for(TRow resultRow : resultRows){
                System.out.println(resultRow.toString());
            }
            
            TCloseOperationReq closeReq = new TCloseOperationReq();
            closeReq.setOperationHandle(stmtHandle);
            client.CloseOperation(closeReq);
            TCloseSessionReq closeConnectionReq = new TCloseSessionReq(sessHandle);
            client.CloseSession(closeConnectionReq);
            
            transport.close();    
            
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
