package com.example.blazegraph.test;

import com.bigdata.Banner;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.ITx;
import com.bigdata.journal.Journal;
import com.bigdata.journal.TimestampUtility;
import com.bigdata.rdf.sail.webapp.ConfigParams;
import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import com.bigdata.util.httpd.Config;
import org.eclipse.jetty.server.Server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class CustomBlazegraphServer extends NanoSparqlServer {

    public void start(String[] args) throws Exception {
        Banner.banner();

        int port = -1;
        String namespace = "kb";
        int queryThreadPoolSize = ConfigParams.DEFAULT_QUERY_THREAD_POOL_SIZE;
        boolean forceOverflow = false;
        Long readLock = null;

        /*
         * Note: This default will locate the jetty.xml resource that is bundled
         * with the JAR. This preserves the historical behavior. If you want to
         * use a different jetty.xml file, just override this property on the
         * command line.
         */
        String jettyXml = System.getProperty(//
                SystemProperties.JETTY_XML,//
                "jetty.xml"//
//                SystemProperties.DEFAULT_JETTY_XML
        );

        String propertyFile = System.getProperty(
                SystemProperties.BIGDATA_PROPERTY_FILE ,
                "RWStore.properties"
        );

        String portStr = System.getProperty(
                SystemProperties.JETTY_PORT ,
                Integer.toString(Config.BLAZEGRAPH_HTTP_PORT)
        );

        port = Integer.parseInt(portStr);

        /*
         * Handle all arguments starting with "-". These should appear before
         * any non-option arguments to the program.
         */
        int i = 0;
        while (i < args.length) {
            final String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.equals("-forceOverflow")) {
                    forceOverflow = true;
                } else if (arg.equals("-nthreads")) {
                    final String s = args[++i];
                    queryThreadPoolSize = Integer.valueOf(s);
                    if (queryThreadPoolSize < 0) {
                        usage(1/* status */,
                                "-nthreads must be non-negative, not: " + s);
                    }
                } else if (arg.equals("-readLock")) {
                    final String s = args[++i];
                    readLock = Long.valueOf(s);
                    if (readLock != ITx.READ_COMMITTED
                            && !TimestampUtility.isCommitTime(readLock
                            .longValue())) {
                        usage(1/* status */,
                                "Read lock must be commit time or -1 (MINUS ONE) to assert a read lock on the last commit time: "
                                        + readLock);
                    }
                } else if (arg.equals("-jettyXml")) {
                    jettyXml = args[++i];
                } else {
                    usage(1/* status */, "Unknown argument: " + arg);
                }
            } else {
                break;
            }
            i++;
        }

        final Map<String, String> initParams = new LinkedHashMap<String, String>();

        initParams.put(
                ConfigParams.PROPERTY_FILE,
                propertyFile);

        initParams.put(ConfigParams.NAMESPACE,
                namespace);

        initParams.put(ConfigParams.QUERY_THREAD_POOL_SIZE,
                Integer.toString(queryThreadPoolSize));

        initParams.put(
                ConfigParams.FORCE_OVERFLOW,
                Boolean.toString(forceOverflow));

        if (readLock != null) {
            initParams.put(
                    ConfigParams.READ_LOCK,
                    Long.toString(readLock));
        }

        //Set the resource base to inside of the jar file
        System.setProperty("jetty.home",
                jettyXml.getClass().getResource("/war").toExternalForm());

        // Create the service.

        final Properties journalProperties = new Properties();
        {
            journalProperties.setProperty(Journal.Options.BUFFER_MODE,
                    BufferMode.MemStore.name());
        }

        Journal m_indexManager = new Journal(journalProperties);

        final Server server = CustomBlazegraphServer.newInstance(port, jettyXml,
                null/* indexManager */, initParams);


        server.start();
        awaitServerStart(server);

        System.out.println("\n\nWelcome to the Blazegraph(tm) Database.\n");
        //BLZG-1812:  Updated for correction of port override.
        System.out.println("Go to http://" + getHost() + ":" + port + "/" + Config.BLAZEGRAPH_PATH + "/ to get started.");

        // Wait for the service to terminate.
        server.join();
    }
}
