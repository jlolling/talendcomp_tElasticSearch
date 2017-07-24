package de.jlo.talendcomp.elasticsearch;

public class OneValue {
	
	private String field = null;
	private Object value = null;
	private boolean isKey = false;
	
	public String getField() {
		return field;
	}
	
	public void setField(String field) {
		if (field == null || field.trim().isEmpty()) {
			throw new IllegalArgumentException("field cannot be null or empty!");
		}
		this.field = field;
	}
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isKey() {
		return isKey;
	}

	public void setKey(boolean isKey) {
		this.isKey = isKey;
	}

}
