package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.trade.delivery.Delivery;
import org.skysongdev.skysongEconomy.trade.delivery.DeliveryRepository;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncDeliveryRepository implements DeliveryRepository {
    private final DeliveryRepository delegate;
    private final BackgroundWriter writer;

    public AsyncDeliveryRepository(DeliveryRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Delivery> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(Delivery delivery) {
        writer.run("save delivery " + delivery.id(), () -> delegate.save(delivery));
    }

    @Override
    public void delete(String id) {
        writer.run("delete delivery " + id, () -> delegate.delete(id));
    }
}
