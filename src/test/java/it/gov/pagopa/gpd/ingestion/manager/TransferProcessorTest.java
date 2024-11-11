package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.TransferStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.utils.ObjectMapperUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
    private ArgumentCaptor<List<DataCaptureMessage<Transfer>>> transferCaptor;

    @Test
    void runOk() {
        List<DataCaptureMessage<Transfer>> tList = Collections.singletonList(generateValidTransfer());
        String transferItems = ObjectMapperUtils.writeValueAsString(tList);

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCaptureMessage<Transfer>>> documentdb = (OutputBinding<List<DataCaptureMessage<Transfer>>>) spy(OutputBinding.class);

        function = new TransferProcessor();

        // test execution
        assertDoesNotThrow(() -> function.processTransfer(transferItems, documentdb, context));

        verify(documentdb).setValue(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue().get(0);
        assertEquals(tList.get(0).getBefore().getTransferId(), captured.getBefore().getTransferId());
    }

    private DataCaptureMessage<Transfer> generateValidTransfer() {
        Transfer tr = Transfer.builder()
                .amount(0)
                .category("category")
                .transferId("transferId")
                .insertedDate(new Date().getTime())
                .iuv("iuv")
                .lastUpdateDate(new Date().getTime())
                .organizationFiscalCode("organizationFiscalCode")
                .status(TransferStatus.T_REPORTED)
                .build();

        return DataCaptureMessage.<Transfer>builder()
                .before(tr)
                .after(tr)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
