package com.kaleidoscopedoll.render;

import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public interface LegacyGeoState extends GeoRenderState {
    GeoRenderState.Impl geckoState();

    @Override
    default <D> void addGeckolibData(DataTicket<D> ticket, D data) {
        geckoState().addGeckolibData(ticket, data);
    }

    @Override
    default boolean hasGeckolibData(DataTicket<?> ticket) {
        return geckoState().hasGeckolibData(ticket);
    }

    @Override
    default <D> D getGeckolibData(DataTicket<D> ticket) {
        return geckoState().getGeckolibData(ticket);
    }

    @Override
    default java.util.Map<DataTicket<?>, Object> getDataMap() {
        return geckoState().getDataMap();
    }
}
