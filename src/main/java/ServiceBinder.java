import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import config.AppConfig;

public class ServiceBinder extends AbstractModule{

    @Provides @Singleton
    public AppConfig provideAppConfig(){

        return new AppConfig(1, 600);
    }

    @Override
    protected void configure() {

    }
}
