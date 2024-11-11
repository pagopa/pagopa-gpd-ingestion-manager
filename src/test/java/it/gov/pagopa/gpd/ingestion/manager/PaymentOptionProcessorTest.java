package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentOptionStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCapturePaymentOption;
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
class PaymentOptionProcessorTest {

    private PaymentOptionProcessor function;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<List<DataCapturePaymentOption>> paymentOptionCaptor;

    @Test
    void runOk() {

        List<DataCapturePaymentOption> paymentOptionItems = new ArrayList<>();
        paymentOptionItems.add(generateValidPaymentOption());

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentOption>> documentdb = (OutputBinding<List<DataCapturePaymentOption>>) spy(OutputBinding.class);

        function = new PaymentOptionProcessor();

        // test execution
        assertDoesNotThrow(() -> function.processPaymentOption(paymentOptionItems, documentdb, context));

        verify(documentdb).setValue(paymentOptionCaptor.capture());
        DataCapturePaymentOption captured = paymentOptionCaptor.getValue().get(0);
        assertEquals(paymentOptionItems.get(0), captured);
    }

    private DataCapturePaymentOption generateValidPaymentOption() {
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

        return DataCapturePaymentOption.<PaymentOption>builder()
                .before(po)
                .after(po)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
