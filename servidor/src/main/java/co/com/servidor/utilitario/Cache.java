package co.com.servidor.utilitario;

import java.util.HashMap;

public class Cache {
 
    private HashMap<String, Object> cache;
    
    public Cache(){
        this.cache = new HashMap();
    }
    
    public Object getCacheVar(String key) {
        return this.cache.get(key);
    }
    
    public void setCacheVar(String key, Object value){
        this.cache.put(key, value);
    }
    
    public boolean enCache(String key){
        return this.cache.containsKey(key);
    }
    
    public void removerCache(String key){
        this.cache.remove(key);
    }
    
}
