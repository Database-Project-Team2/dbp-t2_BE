package com.dbpt2.server.client;

/** 발주처 응답. project_count는 요청자의 조회 범위 안에서 센다. */
public record ClientResponse(int clientId, String clientName, String contactName, String contactEmail,
                             String contactPhoneNumber, long projectCount) {

    public static ClientResponse of(Client c, long projectCount) {
        return new ClientResponse(c.getId(), c.getName(), c.getContactName(), c.getContactEmail(), c.getContactPhoneNumber(), projectCount);
    }
}
