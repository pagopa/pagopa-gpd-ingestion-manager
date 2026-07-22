package it.gov.pagopa.gpd.ingestion.manager.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureTableConfig {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.table-name}")
    private String tableName;

    @Bean
    public TableClient tableClient() {
        return new TableClientBuilder()
                .connectionString(connectionString)
                .tableName(tableName)
                .buildClient();
    }
}