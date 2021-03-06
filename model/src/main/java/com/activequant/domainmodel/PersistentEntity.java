package com.activequant.domainmodel;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import com.activequant.domainmodel.annotations.Property;

@Entity
@MappedSuperclass
public abstract class PersistentEntity {

	@Column
    private String className;
	@Column
    private long creationTime = System.currentTimeMillis();
	@Column
    private long deletionTime = 0L;
	@Column
    private long snapshotTime = 0L;
	
    private Map<String, Object> underlyingMap = new HashMap<String, Object>();

    public PersistentEntity() {

    }

    protected void clearMap() {
        underlyingMap.clear();
    }

    public PersistentEntity(String className) {
        this.className = className;
    }

    @Property
    public String getClassName() {
        return className;
    }

    @Property
    public long getCreationTime() {
        return creationTime;
    }

    @Property
    public long getDeletionTime() {
        return deletionTime;
    }

    public abstract String getId();

    /**
     *  
     * @return when this value was stored, in ordinary milliseconds. 
     */
    @Property
    public long getSnapshotTime() {
        return snapshotTime;
    }

    /**
     * very underdeveloped converter. 
     * 
     * @param targetType
     * @param input
     * @return
     */
    public Object convertValueToType(Class<?> targetType, Object input)
    {
    	if(input.getClass().isAssignableFrom(targetType))
    		return input;
    	else if(targetType.isAssignableFrom(double.class) || targetType.isAssignableFrom(Double.class)){
    		if(input.getClass().isAssignableFrom(String.class))
    			return Double.parseDouble(input.toString());    		
    	}
    	else if(targetType.isAssignableFrom(long.class) || targetType.isAssignableFrom(Long.class)){
    		if(input.getClass().isAssignableFrom(String.class))
    			return Long.parseLong(input.toString());    
    	}
    	else if(targetType.isAssignableFrom(int.class) || targetType.isAssignableFrom(Integer.class)){
    		if(input.getClass().isAssignableFrom(String.class))
    			return Integer.parseInt(input.toString());  
    		if(input.getClass().isAssignableFrom(Long.class)){
    			return ((Long)input).intValue();
    		}
    	}
    	else if(targetType.isAssignableFrom(TimeStamp.class)){
    		return new TimeStamp((Long)input);
    	}
    	return input; 
    }
    
    /**
     * 
     * @param inMap
     *            the incoming map will be cloned.
     */
    public void initFromMap(Map<String, Object> inMap) {
        // clones the incoming map.
        underlyingMap = new HashMap<String, Object>(inMap);
        Method[] methods = this.getClass().getMethods();
        Map<String, Method> methodMap = new HashMap<String, Method>();
        for (Method m : methods) {
            if (m.getName().startsWith("get") && m.isAnnotationPresent(Property.class)) {
                String propertyName = m.getName().substring(3).toUpperCase();
                if (underlyingMap.containsKey(propertyName)) {
                    @SuppressWarnings("rawtypes")
                    Class returnType = m.getReturnType();
                    // if(returnType.isArray())continue;
                    Object value = underlyingMap.get(propertyName);                    
                    try {
                        Method setter = this.getClass().getMethod("set" + m.getName().substring(3), returnType);
                        // only use values if they are not null and are thus,
                        // present.
                        if (value != null){
                        	// 
                        	Object convertedType = convertValueToType(returnType, value);
                        	// 
                            setter.invoke(this, convertedType);
                        }
                    } catch (Exception ex) {
                        // drop the value.
                    //    log.warn("Could not set " + m.getName().substring(3) + " for value :" + value);                        
                    }
                }
            } else if (m.getName().startsWith("set")) {
                String propertyName = m.getName().substring(3).toUpperCase();
                methodMap.put(propertyName, m);
            }
        }
        // now iterate over the inMap to check for arrays.
        Iterator<Entry<String, Object>> entries = inMap.entrySet().iterator();
        Map<String, Object[]> arrayMap = new HashMap<String, Object[]>();
        while (entries.hasNext()) {
            Entry<String, Object> entry = entries.next();
            if (entry.getKey().startsWith("[")) {
                String[] s = entry.getKey().split(";");
                String key = s[0].substring(1);
                int position = Integer.valueOf(s[1]);
                int totalAmount = Integer.valueOf(s[2]);

                if (!arrayMap.containsKey(key)) {
                    // have to finish this ......
                    Method setter = methodMap.get(key);
                    @SuppressWarnings("rawtypes")
                    Class[] params = setter.getParameterTypes();
                    if (params != null && params.length == 1) {
                        // generate an array of this type.
                        Object array = Array.newInstance(params[0].getComponentType(), totalAmount);

                        // Object[] array =
                        // params[0].getClass().newInstance()[totalAmount];
                        arrayMap.put(key, (Object[]) array);
                    }
                }
                if (arrayMap.containsKey(key)) {
                    Object[] array = arrayMap.get(key);
                    array[position] = entry.getValue();
                }
            }
        }

        // OK, all arrays initialized and temporarily stored.
        Iterator<Entry<String, Object[]>> arrayIter = arrayMap.entrySet().iterator();
        while (arrayIter.hasNext()) {
            Entry<String, Object[]> entry = arrayIter.next();
            //
            Method m = methodMap.get(entry.getKey());
            if (m != null) {
                try {
                    Object[] obj = entry.getValue();
                    m.invoke(this, new Object[] { obj });
                } catch (Exception ex) {
                    // drop the value.
                    // log.warn("Could not set " + m.getName().substring(3) + " for value :" + entry.getValue());
                    ex.printStackTrace();
                }
            }
        }
        
        // weeding out load time artifacts. 
        Iterator<Entry<String, Object>> it = underlyingMap.entrySet().iterator();
        List<String> weedOutKeys = new ArrayList<String>();
    	while(it.hasNext()){
    		Entry<String, Object> entry = it.next();
    		if(entry.getKey().startsWith("["))
    			weedOutKeys.add(entry.getKey());
    	}
    	for(String s : weedOutKeys)
    		underlyingMap.remove(s);
    }

    public String nullSafe(Object val) {
        if (val == null)
            return "<NA>";
        String valString = val.toString();
        // valString.replaceAll(" ", "_");
        return valString; 
    }

    public String nullSafeNoSpace(Object val) {
        if (val == null)
            return "<NA>";
        String valString = val.toString();
        valString.replaceAll(" ", "_");
        return valString; 
    }

    
    /**
     * recreates the entire property map and stores it in the underlying map. 
     * use getUnderlyingMap() for consecutive calls. 
     * 
     * @return the underlying map
     */
    public Map<String, Object> propertyMap() {
        //
        
    	
        
        
        Method[] methods = this.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("get") && m.isAnnotationPresent(Property.class)) {
                try {
                    String propertyName = m.getName().substring(3).toUpperCase();
                    Object value = m.invoke(this);
                    underlyingMap.put(propertyName, value);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        Map<String, Object> ret = new HashMap<String, Object>(underlyingMap);
        return ret;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setDeletionTime(long deletionTime) {
        this.deletionTime = deletionTime;
    }

    public void setSnapshotTime(long snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    /**
     * Use this if you have called property map before and no properties changed in between. 
     * 
     * @return a map with all properties
     */
	public Map<String, Object> getUnderlyingMap() {
		return underlyingMap;
	}

}
