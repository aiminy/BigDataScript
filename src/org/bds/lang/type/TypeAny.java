package org.bds.lang.type;

/**
 * 'Any' value: Generic
 *
 * @author pcingola
 *
 */
public class TypeAny extends TypeUniqueValue {

	public TypeAny() {
		super(PrimitiveType.ANY);
	}

	@Override
	public boolean isAny() {
		return true;
	}

}
