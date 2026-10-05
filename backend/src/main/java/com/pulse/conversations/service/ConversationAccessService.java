package com.pulse.conversations.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.conversations.entity.Conversation;
import com.pulse.conversations.repository.ConversationMemberRepository;
import com.pulse.conversations.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationAccessService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    /** "Not found" for both a missing conversation and one the user is not in, so ids cannot be probed. */
    public Conversation requireMember(UUID conversationId, UUID userId) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found");
        }
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found"));
    }
}
