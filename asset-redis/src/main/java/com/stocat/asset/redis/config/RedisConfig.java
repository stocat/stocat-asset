package com.stocat.asset.redis.config;

import com.stocat.asset.redis.constants.CryptoKeys;
import com.stocat.asset.redis.constants.ExchangeRateKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 연결 설정을 담당합니다.
 */
@Slf4j
@Configuration
@EnableAutoConfiguration(exclude = { RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class })
public class RedisConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    /**
     * Redis 연결 팩토리를 생성합니다.
     *
     * @return LettuceConnectionFactory 인스턴스
     */
    @Primary
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(props.getHost());
        config.setPort(props.getPort());
        config.setPassword(props.getPassword());
        config.setDatabase(props.getDatabase());
        return new LettuceConnectionFactory(config);
    }

    /**
     * 문자열 기반 Redis 템플릿을 생성합니다.
     *
     * @param factory Redis 연결 팩토리
     * @return redisTemplate 인스턴스
     */
    @Primary
    @Bean
    public ReactiveStringRedisTemplate reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(
                factory,
                RedisSerializationContext.string());
    }

    @Primary
    @Bean
    public ReactiveRedisMessageListenerContainer redisContainer(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }

    @Primary
    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic cryptoTradesTopic() {
        return new ChannelTopic(CryptoKeys.CRYPTO_TRADES);
    }

    @Bean
    public ChannelTopic exchangeRatesTopic() {
        return new ChannelTopic(ExchangeRateKeys.EXCHANGE_RATES);
    }
}
