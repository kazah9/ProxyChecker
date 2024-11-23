package com.proxychecker.ui;

import com.proxychecker.dto.ProxyDto;
import com.proxychecker.service.ProxyCheckerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.proxychecker.constants.AppConstants.RESOURCE_GITHUB;
import static com.proxychecker.constants.AppConstants.RESOURCE_PROXY_LIST;

@Route( "" )
public class ProxyCheckerUI extends VerticalLayout {

    private final ProxyCheckerService proxyCheckerService;

    // Компоненты UI
    private final Div resultDiv;
    private final ComboBox<String> proxyTypeComboBox;
    private final ComboBox<String> resourceComboBox;

    // Таблица для отображения прокси
    private final Grid<ProxyDto> proxyGrid;

    @Autowired
    public ProxyCheckerUI( ProxyCheckerService proxyCheckerService ) {
        this.proxyCheckerService = proxyCheckerService;

        // Инициализация компонентов UI
        proxyGrid = new Grid<>( ProxyDto.class );
        proxyGrid.setColumns( "host", "port", "proxyType", "responseTime", "country" );
        proxyGrid.addThemeVariants( GridVariant.LUMO_NO_BORDER );

        // Настройка отображения времени отклика в секундах с тремя знаками после запятой
        proxyGrid.getColumnByKey( "responseTime" )
                .setHeader( "Время отклика (с)" )
                .setComparator( ProxyDto::getResponseTime );

        resultDiv = new Div();

        proxyTypeComboBox = new ComboBox<>( "Тип прокси" );
        proxyTypeComboBox.setItems( "HTTP", "SOCKS4", "SOCKS5" ); // Пример типов
        proxyTypeComboBox.setPlaceholder( "Выберите тип прокси" );

        resourceComboBox = new ComboBox<>( "Ресурс для подключения" );
        resourceComboBox.setItems( RESOURCE_GITHUB, RESOURCE_PROXY_LIST ); // Пример ресурсов
        resourceComboBox.setPlaceholder( "Выберите ресурс" );

        Button checkButton = new Button( "Проверить прокси" );
        checkButton.addClickListener( e -> checkProxies() );

        HorizontalLayout horizontalLayout = new HorizontalLayout( proxyTypeComboBox, resourceComboBox, checkButton );
        horizontalLayout.setAlignItems( Alignment.BASELINE );

        add( proxyGrid, horizontalLayout, resultDiv );
    }

    // Метод для проверки прокси
    private void checkProxies() {
        String proxyType = proxyTypeComboBox.getValue();
        String resource = resourceComboBox.getValue();

        if( proxyType == null || resource == null ) {
            resultDiv.setText( "Пожалуйста, выберите тип прокси и ресурс." );
            return;
        }

        // Вызов сервиса для проверки прокси
        try {
            List<ProxyDto> proxies = proxyCheckerService.checkProxies( proxyType, resource );
            if( proxies.isEmpty() ) {
                resultDiv.setText( "Не удалось найти работающие прокси." );
            } else {
                resultDiv.setText( "Прокси проверены успешно." );
                proxyGrid.setItems( proxies );
            }
        } catch( Exception e ) {
            resultDiv.setText( "Ошибка: " + e.getMessage() );
        }
    }
}
