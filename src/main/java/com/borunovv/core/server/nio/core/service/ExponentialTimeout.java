package com.borunovv.core.server.nio.core.service;

/**
 * Для retry-логики.
 * Рассчитывает экспоненциальный таймаут по формуле: T = min(S * (M ^ (N-1)), MaxTimeout)
 * N = [1..Nmax]
 * T - Таймаут (в миллисекундах)
 * S - Начальное значение таймаута.
 * M - Основание степени экспоненты
 * N - Номер попытки
 * Nmax - максимальное кол-во попыток.
 * MaxTimeout - верхнее пороговое значение таймаута.
 */
public class ExponentialTimeout {

    // Начальное значение таймаута (S).
    // (см. формулу выше)
    private final int startTimeoutMs;

    // Экспоненциальный множитель (M).
    private final double exponentBase;

    // Макс. кол-во попыток Nmax.
    private final int maxAttempts;

    // Ограничение на максимальный таймаут (MaxTimeout)
    private final long maxTimeout;

    public ExponentialTimeout(int startTimeoutMs,
                              double exponentBase,
                              int maxAttempts,
                              long maxTimeout) {
        this.startTimeoutMs = startTimeoutMs;
        this.exponentBase = exponentBase;
        this.maxAttempts = maxAttempts;
        this.maxTimeout = maxTimeout;
    }

    // Вернет кол-во миллесекунд ожидания для заданной попытки.
    // Если номер попытки > Nmax, то вернет -1.
    public long getTimeout(int attemptNumber) {
        if (attemptNumber > maxAttempts) {
            return -1;
        }

        long cooldownMs = (long) (startTimeoutMs
                * Math.pow(exponentBase, attemptNumber - 1));

        cooldownMs = Math.min(maxTimeout, cooldownMs);
        return cooldownMs;
    }
}
