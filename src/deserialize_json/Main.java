package deserialize_json;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Field;

import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
	static class Debug {
	    public String toString() {
	    	return debugString(0);
	    }

	    public String debugString(int indentCount) {
	    	String indent = "";
	    	for(int i=0; i<indentCount; i++) {
	    		indent += "  ";
	    	}

	    	String ret = "";
	    	String className = getClass().getName().split("\\$")[1];
	    	ret += className + " {\n";
    		Field[] fields = getClass().getDeclaredFields();
    		for(Field f: fields){
    			String fieldName = f.getName();
    			Object value;
    			try {
    				value = f.get(this);
    			} catch(IllegalAccessException e) {
    				e.printStackTrace();
    				return super.toString();
    			}
    			String valueString = value instanceof Debug
    				? ((Debug)value).debugString(indentCount+1)
    				: value.toString();
    			ret += indent + "  " + fieldName + ": " + valueString + "\n";
    		}
	    	ret += indent + "}";
	    	return ret;
		}
	}

	static class MyClass extends Debug {
	    // リスト型のみ
	    Integer integer_field;
	    Boolean boolean_field;
	    MyClass2 my_class2;
	    MyClass3 my_class3;
	}

	static class MyClass2 extends Debug {
	    Integer integer_field;
	    Double double_field;
	    String string_field;
	}

	static class MyClass3 extends Debug {
		List<String> string_list;
		HashMap<String, String> str_str_map;
		HashMap<String, Integer> str_int_map;
	}

	public static void main(String[] args) {
		String json_string = "";
		try {
			json_string = stringFromFile("struct.json");
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		JSONObject json = new JSONObject(json_string);

		System.out.println("deserializing...\n");
		MyClass obj = deserialize(MyClass.class, json);

		System.out.println(obj);
	}

	public static String stringFromFile(final String path) throws IOException {
		return Files.lines(Paths.get(path), Charset.forName("UTF-8"))
		        .collect(Collectors.joining(System.getProperty("line.separator")));
	}

	public static <T> T deserialize(Class<T> clazz, JSONObject json) {
		T ret_obj;
		try {
			ret_obj = (T)clazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	    try {
	    	Field[] fields = ret_obj.getClass().getDeclaredFields();
	    	for(Field f: fields){
	    		String field_name = f.getName();
	    		Object obj;
	    		try {
	    			obj = json.get(field_name);
	    		} catch(JSONException e) {
	    			continue;
	    		}
    			Class<?> type = f.getType();
    			if (obj instanceof JSONObject) {
    				JSONObject json_obj = (JSONObject)obj;
    				if (type == HashMap.class) {
    					f.set(ret_obj, json_obj.toMap());
    				} else {
	    				Object value = deserialize(type, (JSONObject)json_obj);
	    				try {
	    					f.set(ret_obj, type.cast(value));
	    				} catch(ClassCastException e) {
	    					e.printStackTrace();
	    					f.set(ret_obj, null);
	    				}
    				}
    			} else if (obj instanceof JSONArray) {
    				JSONArray json_array = (JSONArray)obj;
    				f.set(ret_obj, json_array.toList());
    			} else {
    				try {
    					f.set(ret_obj, type.cast(obj));
    				} catch(ClassCastException e) {
    					e.printStackTrace();
    					f.set(ret_obj, null);
    				}
    			}
	    	}
    	} catch(IllegalAccessException e) {
    		e.printStackTrace();
    		System.exit(1);
    	}
	    return ret_obj;
	}
}