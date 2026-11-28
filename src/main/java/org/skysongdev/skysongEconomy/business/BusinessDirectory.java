package org.skysongdev.skysongEconomy.business;

import org.skysongdev.skysongEconomy.api.business.BusinessBlock;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;
import org.skysongdev.skysongEconomy.api.business.Businesses;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Optional;

/** The {@link Businesses} service other plugins load: read-only copies of {@link BusinessService}'s businesses. */
public final class BusinessDirectory implements Businesses {
    private final BusinessService businesses;

    public BusinessDirectory(BusinessService businesses) {
        this.businesses = businesses;
    }

    /** @return the public copy of a business, as it is now */
    public static BusinessInfo info(Business business) {
        return new BusinessInfo(business.id(), business.typeId(), business.displayName(), business.tier(),
                business.owner(), business.workers().stream().map(Hire::character).toList(),
                business.position().map(BusinessDirectory::block));
    }

    private static BusinessBlock block(BlockPosition position) {
        return new BusinessBlock(position.world(), position.x(), position.y(), position.z());
    }

    @Override
    public Optional<BusinessInfo> find(String businessId) {
        return businesses.find(businessId).map(BusinessDirectory::info);
    }

    @Override
    public List<BusinessInfo> all() {
        return businesses.all().stream().map(BusinessDirectory::info).toList();
    }

    @Override
    public List<BusinessInfo> ownedBy(CharacterRef owner) {
        return businesses.all().stream().filter(business -> business.isOwner(owner)).map(BusinessDirectory::info).toList();
    }
}
