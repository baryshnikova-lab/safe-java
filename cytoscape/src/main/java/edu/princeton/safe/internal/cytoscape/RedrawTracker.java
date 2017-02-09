package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.view.model.events.UpdateNetworkPresentationEvent;
import org.cytoscape.view.model.events.UpdateNetworkPresentationListener;

import edu.princeton.safe.internal.cytoscape.event.EventService;

public class RedrawTracker implements UpdateNetworkPresentationListener {

    private EventService eventService;

    public RedrawTracker(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void handleEvent(UpdateNetworkPresentationEvent event) {
        eventService.notifyPresentationStateChanged(true);
    }

}
