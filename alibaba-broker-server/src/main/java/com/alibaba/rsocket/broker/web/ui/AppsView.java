package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.model.AppInstance;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.broker.web.ui.AppsView.NAV;

/**
 * Apps view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class AppsView extends VerticalLayout {
    public static final String NAV = "AppsView";
    private Grid<AppInstance> appMetadataGrid = new Grid<>();
    private RSocketBrokerHandlerRegistry handlerRegistry;

    public AppsView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
        add(new H1("App Instances List"));
        appMetadataGrid.addColumn(AppInstance::getName).setHeader("App Name");
        appMetadataGrid.addColumn(AppInstance::getOrgs).setHeader("Organizations");
        appMetadataGrid.addColumn(AppInstance::getServiceAccounts).setHeader("ServiceAccounts");
        appMetadataGrid.addColumn(AppInstance::getRoles).setHeader("Roles");
        appMetadataGrid.addColumn(AppInstance::getIp).setHeader("IP");
        appMetadataGrid.addColumn(AppInstance::getConnectedAt).setHeader("Started Time");
        appMetadataGrid.addColumn(AppInstance::getStatusText).setHeader("Status");
        appMetadataGrid.addColumn(new ComponentRenderer<>(appInstance -> {
                    Checkbox checkbox = new Checkbox(appInstance.getStatus().equals(AppStatusEvent.STATUS_SERVING));
                    checkbox.addValueChangeListener(event -> {
                        RSocketBrokerResponderHandler responderHandler = handlerRegistry.findByUUID(appInstance.getId());
                        if (responderHandler != null) {
                            if (checkbox.getValue()) {
                                responderHandler.registerPublishedServices();
                                appInstance.setStatus(AppStatusEvent.STATUS_SERVING);
                                Notification.show(appInstance.getName() + "'s status is " + appInstance.getStatusText());
                            } else {
                                responderHandler.unRegisterPublishedServices();
                                appInstance.setStatus(AppStatusEvent.STATUS_OUT_OF_SERVICE);
                                Notification.show(appInstance.getName() + "'s status is " + appInstance.getStatusText());
                            }
                        }
                    });
                    return checkbox;
                })
        ).setHeader("Enabled");
        appMetadataGrid.addColumn(TemplateRenderer.<AppInstance>of("<b inner-h-t-m-l='[[item.servicesText]]'></b>")
                .withProperty("servicesText", AppInstance::getServicesHTML)).setHeader("Services");
        appMetadataGrid.addColumn(TemplateRenderer.<AppInstance>of("<b inner-h-t-m-l='[[item.servicesText]]'></b>")
                .withProperty("consumedServicesText", AppInstance::getConsumedServicesHTML)).setHeader("Consumed");
        add(appMetadataGrid);
        AppDetailPanel detailPanel = new AppDetailPanel();
        add(detailPanel);
        appMetadataGrid.addItemClickListener((ComponentEventListener<ItemClickEvent<AppInstance>>) clickEvent -> {
            AppInstance appInstance = clickEvent.getItem();
            detailPanel.setAppName(appInstance.getName());
            detailPanel.setDescription(appInstance.getMetadata().getDescription());
            detailPanel.setHumans(appInstance.getMetadata().getHumansMd());
        });

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        appMetadataGrid.setItems(appMetadataList(handlerRegistry));
    }

    public List<AppInstance> appMetadataList(RSocketBrokerHandlerRegistry handlerFactory) {
        return handlerFactory.findAll()
                .stream()
                .map(handler -> {
                    AppInstance appInstance = new AppInstance();
                    AppMetadata appMetadata = handler.getAppMetadata();
                    appInstance.setId(appMetadata.getUuid());
                    appInstance.setName(appMetadata.getName());
                    appInstance.setOrgs(appMetadata.getMetadata("_orgs"));
                    appInstance.setServiceAccounts(appMetadata.getMetadata("_serviceAccounts"));
                    appInstance.setRoles(appMetadata.getMetadata("_roles"));
                    appInstance.setIp(appMetadata.getIp());
                    appInstance.setStatus(handler.getAppStatus());
                    appInstance.setServices(handler.getPeerServices());
                    appInstance.setConsumedServices(handler.getConsumedServices());
                    appInstance.setConnectedAt(appMetadata.getConnectedAt());
                    appInstance.setMetadata(appMetadata);
                    return appInstance;
                })
                .collect(Collectors.toList());
    }
}
