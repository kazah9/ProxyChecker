package com.proxychecker.ui;

import com.proxychecker.dto.ProxyDto;
import com.proxychecker.service.ProxyCheckerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

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

        resultDiv = new Div();
        proxyGrid = new Grid<>( ProxyDto.class );
        proxyGrid.removeAllColumns();

        proxyGrid.addColumn( new ComponentRenderer<>( proxy -> {
                    Image statusImage = Objects.nonNull( proxy.getResponseTime() ) && proxy.getResponseTime().compareTo( BigDecimal.valueOf( 0.300 ) ) < 0
                            ? new Image( "images/ok.png", "OK" )
                            : new Image( "images/warning.png", "Warning" );

                    statusImage.setWidth( "14px" );
                    statusImage.setHeight( "14px" );
                    return statusImage;
                } ) ).setHeader( "Статус" )
                .setTextAlign( ColumnTextAlign.CENTER )
                .setWidth( "75px" )
                .setFlexGrow( 0 );

        proxyGrid.addColumn( proxy -> proxy.getHost() + ":" + proxy.getPort() )
                .setHeader( "IP:Порт" )
                .setTextAlign( ColumnTextAlign.CENTER )
                .setWidth( "225px" )
                .setFlexGrow( 0 );

        proxyGrid.addColumn( ProxyDto::getProxyType )
                .setHeader( "Тип прокси" )
                .setTextAlign( ColumnTextAlign.CENTER )
                .setComparator( ProxyDto::getProxyType )
                .setWidth( "125px" )
                .setFlexGrow( 0 );

        proxyGrid.addColumn( ProxyDto::getResponseTime )
                .setHeader( "Время отклика (с)" )
                .setTextAlign( ColumnTextAlign.CENTER )
                .setComparator( ProxyDto::getResponseTime )
                .setWidth( "175px" )
                .setFlexGrow( 0 );

        proxyGrid.addColumn( ProxyDto::getCountry )
                .setHeader( "Страна" )
                .setTextAlign( ColumnTextAlign.CENTER )
                .setComparator( ProxyDto::getCountry )
                .setWidth( "100px" )
                .setFlexGrow( 0 );

        proxyGrid.addThemeVariants( GridVariant.LUMO_NO_BORDER );
        proxyGrid.setHeight( "700px" );

        proxyTypeComboBox = new ComboBox<>( "Тип прокси" );
        proxyTypeComboBox.setItems( "HTTP", "SOCKS4", "SOCKS5" );
        proxyTypeComboBox.setPlaceholder( "Выберите тип прокси" );

        resourceComboBox = new ComboBox<>( "Ресурс для подключения" );
        resourceComboBox.setItems( RESOURCE_GITHUB, RESOURCE_PROXY_LIST );
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
