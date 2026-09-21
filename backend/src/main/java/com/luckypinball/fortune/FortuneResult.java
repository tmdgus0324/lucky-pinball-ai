package com.luckypinball.fortune;

/**
 * {@link FortuneService}가 만들어내는 원시 결과 (버프/영속화 이전 단계).
 */
public record FortuneResult(int fortuneScore, int luckyNumber, String fortuneMessage) {
}
