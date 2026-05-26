package com.matador.customer.internal;

import com.matador.customer.Customer;
import com.matador.customer.api.CustomerDtos.AddressResponse;
import com.matador.customer.api.CustomerDtos.AdminCustomerDetail;
import com.matador.customer.api.CustomerDtos.AdminCustomerSummary;
import com.matador.customer.api.CustomerDtos.CustomerProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CustomerMapper {

    @Mapping(target = "canBook", expression = "java(customer.canBook())")
    CustomerProfileResponse toProfile(Customer customer);

    AdminCustomerSummary toSummary(Customer customer);

    AdminCustomerDetail toDetail(Customer customer);

    @Mapping(target = "lat", expression = "java(com.matador.shared.geo.GeoSupport.lat(address.getLocation()))")
    @Mapping(target = "lng", expression = "java(com.matador.shared.geo.GeoSupport.lng(address.getLocation()))")
    @Mapping(target = "isDefault", source = "default")
    AddressResponse toAddressResponse(CustomerAddress address);
}
