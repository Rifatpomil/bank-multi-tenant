package org.axonframework.samples.bank.web;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.samples.bank.query.bankaccount.BankAccountRepository;
import org.axonframework.samples.bank.web.dto.DepositDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BankAccountControllerTest {

    @Autowired
    private BankAccountController controller;

    @MockBean
    private CommandGateway commandGateway;

    @MockBean
    private BankAccountRepository repository;

    @Autowired
    private Validator validator;

    @Test
    public void testDeposit_ValidationFails_NegativeAmount() {
        DepositDto dto = new DepositDto("acc-1", -100);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "depositDto");
        validator.validate(dto, errors);
        
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.getFieldError("amount")).isNotNull();
    }

    @Test
    public void testDeposit_ValidationFails_EmptyAccountId() {
        DepositDto dto = new DepositDto("", 100);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "depositDto");
        validator.validate(dto, errors);
        
        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.getFieldError("bankAccountId")).isNotNull();
    }

    @Test
    public void testDeposit_ValidationPasses_ValidInput() {
        DepositDto dto = new DepositDto("acc-1", 100);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "depositDto");
        validator.validate(dto, errors);
        
        assertThat(errors.hasErrors()).isFalse();
    }
}
