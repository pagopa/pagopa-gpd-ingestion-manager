package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.TransferStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class TransferProcessorTest {
    private TransferProcessor function;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<List<Transfer>> transferCaptor;

    @Test
    void runOk() {

        List<Transfer> transferItems = new ArrayList<>();
        transferItems.add(generateValidTransfer());

        @SuppressWarnings("unchecked")
        OutputBinding<List<Transfer>> documentdb = (OutputBinding<List<Transfer>>) spy(OutputBinding.class);

        function = new TransferProcessor();

        // test execution
        assertDoesNotThrow(() -> function.processTransfer(transferItems, documentdb, context));

        verify(documentdb).setValue(transferCaptor.capture());
        Transfer captured = transferCaptor.getValue().get(0);
        assertEquals(transferItems.get(0), captured);
    }

    private Transfer generateValidTransfer() {

        return Transfer.builder()
                .amount(0)
                .category("category")
                .transferId("transferId")
                .insertedDate(new Date().getTime())
                .iuv("iuv")
                .lastUpdateDate(new Date().getTime())
                .organizationFiscalCode("organizationFiscalCode")
                .status(TransferStatus.T_REPORTED)
                .build();
    }
}
