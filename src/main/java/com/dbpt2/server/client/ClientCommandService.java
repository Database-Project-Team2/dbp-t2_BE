package com.dbpt2.server.client;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.security.AuthUser;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 발주처 등록·수정(경영진 전용). 대표 담당자는 한 명이며 변경 이력은 남기지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional
public class ClientCommandService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\- ]{1,30}$");

    private final ClientRepository clientRepository;
    private final AccessPolicy accessPolicy;

    public record CreateClientRequest(String clientName, String contactName, String contactEmail, String contactPhoneNumber) {}

    public int create(AuthUser user, CreateClientRequest r) {
        accessPolicy.requireManagement(user);
        return clientRepository.save(Client.create(name(r.clientName()), contactName(r.contactName()), email(r.contactEmail()),
                phone(r.contactPhoneNumber()))).getId();
    }

    public void update(AuthUser user, int clientId, PatchBody patch) {
        accessPolicy.requireManagement(user);
        Client c = clientRepository.findById(clientId).orElseThrow(ApiException::notFound);
        c.update(
                patch.has("client_name") ? name(patch.text("client_name", false)) : c.getName(),
                patch.has("contact_name") ? contactName(patch.text("contact_name", false)) : c.getContactName(),
                patch.has("contact_email") ? email(patch.text("contact_email", false)) : c.getContactEmail(),
                patch.has("contact_phone_number") ? phone(patch.text("contact_phone_number", true)) : c.getContactPhoneNumber());
    }

    private static String name(String raw) {
        return required(raw, "client_name", 150, "발주처명을 1~150자로 입력해 주세요.");
    }

    private static String contactName(String raw) {
        return required(raw, "contact_name", 100, "담당자명을 1~100자로 입력해 주세요.");
    }

    private static String email(String raw) {
        String value = required(raw, "contact_email", 254, "담당자 이메일을 입력해 주세요.");
        if (!EMAIL.matcher(value).matches()) {
            throw ApiException.invalid("contact_email", "올바른 이메일을 입력해 주세요.");
        }
        return value;
    }

    private static String phone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (!PHONE.matcher(value).matches()) {
            throw ApiException.invalid("contact_phone_number", "전화번호는 숫자·하이픈 등 30자 이하로 입력해 주세요.");
        }
        return value;
    }

    private static String required(String raw, String field, int max, String message) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > max) {
            throw ApiException.invalid(field, message);
        }
        return value;
    }
}
