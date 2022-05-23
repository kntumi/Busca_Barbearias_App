package kev.app.timeless.di.viewModelFactory;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider.Factory;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ViewModelProvidersFactory implements Factory {
  private final Map<Class<? extends ViewModel>, Provider<ViewModel>> creators;

  @Inject
  public ViewModelProvidersFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> creators) {
    this.creators = creators;
  }

  @NonNull
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    Provider<? extends ViewModel> creator = (Provider)this.creators.get(modelClass);
    if (creator == null) {
      Iterator var3 = this.creators.entrySet().iterator();

      while(var3.hasNext()) {
        Entry<Class<? extends ViewModel>, Provider<ViewModel>> entry = (Entry)var3.next();
        if (modelClass.isAssignableFrom((Class)entry.getKey())) {
          creator = (Provider)entry.getValue();
          break;
        }
      }
    }

    if (creator == null) {
      throw new IllegalArgumentException("unknown model class " + modelClass);
    } else {
      try {
        return (T) creator.get();
      } catch (Exception var5) {
        throw new RuntimeException(var5);
      }
    }
  }
}