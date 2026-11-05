package org.skysongdev.skysongEconomy.data.datatypes;

import java.util.ArrayList;
import java.util.List;

public class ImportCatalogue {
    private ArrayList<ImportOffer> importOffers;

    public ImportCatalogue(List<ImportOffer> offers) {
        this.importOffers = new ArrayList<>(offers);
    }

    public List<ImportOffer> getOffers() {
        return List.copyOf(importOffers);
    }
}
