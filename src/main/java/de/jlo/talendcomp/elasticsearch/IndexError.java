package de.jlo.talendcomp.elasticsearch;

public class IndexError {
	
	private String operation = null;
	private String id = null;
	private String message = null;
	
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof IndexError) {
			IndexError other = (IndexError) o;
			return other.id.equals(id) && other.operation.equals(operation);
		} else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		return (id + operation).hashCode();
	}

}
