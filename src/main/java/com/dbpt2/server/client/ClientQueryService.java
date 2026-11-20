package com.dbpt2.server.client;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.security.AuthUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientQueryService {

    private final ClientRepository clientRepository;
    private final ClientSearch clientSearch;
    private final AccessPolicy accessPolicy;

    public Page<ClientResponse> search(AuthUser user, String query, Pageable pageable) {
        Integer viewer = viewer(user);
        Page<Client> page = clientSearch.search(query, viewer, pageable);
        Map<Integer, Long> counts = projectCounts(page.map(Client::getId).getContent(), viewer);
        return page.map(c -> ClientResponse.of(c, counts.getOrDefault(c.getId(), 0L)));
    }

    /** 경영진 또는 해당 발주처 프로젝트의 구성원. 범위 밖은 404. */
    public ClientResponse detail(AuthUser user, int clientId) {
        Integer viewer = viewer(user);
        if (viewer != null && !clientRepository.isVisibleTo(clientId, viewer)) {
            throw ApiException.notFound();
        }
        Client client = clientRepository.findById(clientId).orElseThrow(ApiException::notFound);
        return ClientResponse.of(client, projectCounts(List.of(clientId), viewer).getOrDefault(clientId, 0L));
    }

    private Integer viewer(AuthUser user) {
        return accessPolicy.isManagement(user) ? null : user.employeeId();
    }

    Map<Integer, Long> projectCounts(List<Integer> clientIds, Integer viewer) {
        Map<Integer, Long> counts = new HashMap<>();
        if (!clientIds.isEmpty()) {
            for (Object[] row : clientRepository.countProjectsByClient(clientIds, viewer)) {
                counts.put((Integer) row[0], ((Number) row[1]).longValue());
            }
        }
        return counts;
    }
}
