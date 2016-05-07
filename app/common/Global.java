package common;


import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;

import java.net.InetAddress;

public class Global extends GlobalSettings {

    private static TransportClient elasticTransportClient;
    private static String esIndexAirport, esTypeAirport;


    public static TransportClient getElasticTransportClient() {
        return elasticTransportClient;
    }

    public static String getEsIndexAirport() {
        return esIndexAirport;
    }

    public static String getEsTypeAirport() {
        return esTypeAirport;
    }


    @Override
    public void onStart(Application config) {
        super.onStart(config);

        Configuration configuration = config.configuration();
        try {
            elasticTransportClient = TransportClient.builder()
                    .settings(Settings.builder()
                            .put("cluster.name", configuration.getString("es.cluster"))
                            .build())
                    .build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(configuration.getString("es.host")), configuration.getInt("es.port")));

            esIndexAirport = configuration.getString("es.indexAirport");
            esTypeAirport = configuration.getString("es.typeAirport");
        } catch (Exception ex) {
            Logger.error(ex.getMessage(), ex);
        }
    }
}