package antock.Antock_Project.external.address;

import antock.Antock_Project.external.address.dto.AddressResponse;

public interface AddressApiClient {
    AddressResponse getAddressInfo(String address);
}
