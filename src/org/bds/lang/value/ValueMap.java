package org.bds.lang.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bds.lang.type.Type;

/**
 * Define a value of type map
 *
 * Note: It is a map of 'value'
 *
 * @author pcingola
 */
public class ValueMap extends ValueComposite {

	private static final long serialVersionUID = -4576221958237314363L;

	Map<Value, Value> map;

	public ValueMap(Type type) {
		super(type);
		map = new HashMap<>();
	}

	public ValueMap(Type type, int size) {
		super(type);
		map = new HashMap<>(size);
	}

	@Override
	public ValueMap clone() {
		ValueMap vm = new ValueMap(type, map.size());
		for (Value vkey : map.keySet())
			vm.put(vkey, getValue(vkey));
		return vm;
	}

	//	@Override
	//	public Map<Value, Value> get() {
	//		return map;
	//	}

	/**
	 * Get element 'key' (which is a 'Value' object)
	 */
	public Value getValue(Value key) {
		return map.get(key);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<Value> keySet() {
		return map.keySet();
	}

	/**
	 * Put (set) entry 'key'
	 */
	public void put(Value key, Value val) {
		map.put(key, val);
	}

	//	@SuppressWarnings("unchecked")
	//	@Override
	//	public void set(Object v) {
	//		// !!! TODO: WE SHOULD PROBABLY NEVER DO THIS
	//		map = (Map<Value, Value>) v;
	//	}

	@Override
	public void setValue(Value vmap) {
		map = ((ValueMap) vmap).map;
	}

	public int size() {
		return map.size();
	}

	@Override
	public String toString() {
		if (isEmpty()) return "{}";

		StringBuilder sb = new StringBuilder();
		List<Value> keys = new ArrayList<>();
		keys.addAll(map.keySet());
		Collections.sort(keys);

		for (Value key : keys) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(" " + key.toString() + " => " + getValue(key));
		}
		return "{" + sb.toString() + " }";
	}
}
