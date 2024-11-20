package com.proxychecker.ui;

import com.proxychecker.service.ProxyCheckerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route( "" )  // Главная страница
public class ProxyCheckerUI extends VerticalLayout {

    private final ProxyCheckerService proxyCheckerService;

    // Компоненты UI
    private TextField proxyTypeField;
    private Button checkButton;
    private Div resultDiv;

    @Autowired
    public ProxyCheckerUI( ProxyCheckerService proxyCheckerService ) {
        this.proxyCheckerService = proxyCheckerService;

        // Инициализация компонентов UI
        proxyTypeField = new TextField( "Тип прокси (например, http или socks4)" );
        checkButton = new Button( "Проверить прокси" );
        resultDiv = new Div();

        // Добавление обработчика нажатия кнопки
        checkButton.addClickListener( e -> checkProxies() );

        // Размещение компонентов на странице
        add( proxyTypeField, checkButton, resultDiv );
    }

    // Метод для проверки прокси
    private void checkProxies() {
        String proxyType = proxyTypeField.getValue();
        if( proxyType.isEmpty() ) {
            resultDiv.setText( "Пожалуйста, укажите тип прокси." );
            return;
        }

        // Вызов сервиса для проверки прокси
        try {
            proxyCheckerService.checkProxies( proxyType );
            resultDiv.setText( "Прокси проверены успешно." );
        } catch( Exception e ) {
            resultDiv.setText( "Ошибка: " + e.getMessage() );
        }
    }
}
