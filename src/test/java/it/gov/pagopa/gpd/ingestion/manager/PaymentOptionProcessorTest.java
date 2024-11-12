package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentOptionStatus;
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
class PaymentOptionProcessorTest {

    private PaymentOptionProcessor function;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<List<DataCaptureMessage<PaymentOption>>> paymentOptionCaptor;

    @Test
    void runOk() {
        List<DataCaptureMessage<PaymentOption>> poList = Collections.singletonList(generateValidPaymentOption());
        String paymentOptionItems = ObjectMapperUtils.writeValueAsString(poList);

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCaptureMessage<PaymentOption>>> documentdb = (OutputBinding<List<DataCaptureMessage<PaymentOption>>>) spy(OutputBinding.class);

        function = new PaymentOptionProcessor();

        // test execution
        assertDoesNotThrow(() -> function.processPaymentOption(paymentOptionItems, documentdb, context));

        verify(documentdb).setValue(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue().get(0);
        assertEquals(poList.get(0).getBefore().getId(), captured.getBefore().getId());
    }

    private DataCaptureMessage<PaymentOption> generateValidPaymentOption() {
        PaymentOption po = PaymentOption.builder()
                .id(0)
                .paymentPositionId(0)
                .amount(0)
                .description("description")
                .dueDate(new Date().getTime())
                .fee(0)
                .flowReportingId("flowReportingId")
                .receiptId("receiptId")
                .insertedDate(new Date().getTime())
                .isPartialPayment(false)
                .iuv("iuv")
                .lastUpdateDate(new Date().getTime())
                .organizationFiscalCode("organizationFiscalCode")
                .status(PaymentOptionStatus.PO_PAID)
                .paymentDate(new Date().getTime())
                .paymentMethod("paymentMethod")
                .pspCompany("pspCompany")
                .reportingDate(new Date().getTime())
                .retentionDate(new Date().getTime())
                .notificationFee(0)
                .lastUpdatedDateNotificationFee(new Date().getTime())
                .build();

        return DataCaptureMessage.<PaymentOption>builder()
                .before(po)
                .after(po)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
