package map.poc2.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import map.poc2.constants.CacheType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public Config hazelCastConfig() {

        Config config = new Config();
        config.setInstanceName(CacheType.HAZELCAST_CACHE.getValue());

        MapConfig buySheet = new MapConfig();
        buySheet.setEvictionPolicy(EvictionPolicy.LRU);
        config.getMapConfigs().put(CacheType.BUY_SHEET.getValue(), buySheet);

        return config;
    }

}