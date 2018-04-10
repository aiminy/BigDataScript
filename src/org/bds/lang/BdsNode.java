package org.bds.lang;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bds.compile.CompilerMessage.MessageType;
import org.bds.compile.CompilerMessages;
import org.bds.compile.TypeCheckedNodes;
import org.bds.lang.statement.StatementInclude;
import org.bds.lang.type.PrimitiveType;
import org.bds.lang.type.Type;
import org.bds.lang.type.TypeList;
import org.bds.lang.type.TypeMap;
import org.bds.lang.type.Types;
import org.bds.run.BdsThread;
import org.bds.symbol.SymbolTable;
import org.bds.util.Timer;

/**
 * Base AST node for bds language elements
 *
 * @author pcingola
 */
public abstract class BdsNode implements Serializable {

	private static final long serialVersionUID = -2443078474175192104L;
	protected BdsNode parent;
	protected int id, lineNum, charPosInLine; // Source code info
	protected Type returnType;

	/**
	 * Constructor
	 * @param parent : Parent node
	 * @param tree   : Not null if you want the parsing to be performed now
	 */
	public BdsNode(BdsNode parent, ParseTree tree) {
		id = BdsNodeFactory.get().getNextNodeId(this);
		this.parent = parent;

		// Initialize some defaults
		initialize();
		doParse(tree);
	}

	public boolean canCastTo(BdsNode n) {
		return returnType != null && returnType.canCastTo(n.getReturnType());
	}

	public boolean canCastToBool() {
		return returnType != null && returnType.canCastTo(Types.BOOL);
	}

	public boolean canCastToInt() {
		return returnType != null && returnType.canCastTo(Types.INT);
	}

	public boolean canCastToReal() {
		return returnType != null && returnType.canCastTo(Types.REAL);
	}

	public boolean canCastToString() {
		return returnType != null && true; // Everything can be cast to a string
	}

	public void checkCanCastTo(Type t, CompilerMessages compilerMessages) {
		if (!returnType.canCastTo(t)) {
			compilerMessages.add(this, "Cannot cast " + returnType + " to " + t, MessageType.ERROR);
		}
	}

	public void checkCanCastToBool(CompilerMessages compilerMessages) {
		checkCanCastTo(Types.BOOL, compilerMessages);
	}

	public void checkCanCastToInt(CompilerMessages compilerMessages) {
		checkCanCastTo(Types.INT, compilerMessages);
	}

	public void checkCanCastToNumeric(CompilerMessages compilerMessages) {
		if (!canCastToInt() && !canCastToReal()) compilerMessages.add(this, "Cannot cast " + returnType + " to int or real", MessageType.ERROR);
	}

	public void checkCanCastToReal(CompilerMessages compilerMessages) {
		checkCanCastTo(Types.REAL, compilerMessages);
	}

	public void checkCanCastToString(CompilerMessages compilerMessages) {
		checkCanCastTo(Types.STRING, compilerMessages);
	}

	/**
	 * Parse tree
	 */
	protected void doParse(ParseTree tree) {
		if (tree != null) {
			lineAndPos(tree); // Add line and char info

			// Parse ANTLR tree
			try {
				parse(tree);
			} catch (Exception e) {
				throw new RuntimeException("Error parsing file '" + getFileName() + "', line " + lineNum + ", char " + charPosInLine + ", node '" + this + "'", e);
			}

			// Sanity checks
			sanityCheck(CompilerMessages.get());
		} else {
			if (parent != null && parent.getLineNum() > 0) lineNum = parent.getLineNum();
		}
	}

	/**
	 * Create a BigDataScriptNode
	 */
	final public BdsNode factory(ParseTree tree, int childNum) {
		ParseTree child = childNum >= 0 ? tree.getChild(childNum) : tree;
		return BdsNodeFactory.get().factory(this, child);
	}

	/**
	 * Finds a terminal node in the tree (by name)
	 */
	protected int findIndex(ParseTree tree, String name, int start) {
		for (int i = start; i < tree.getChildCount(); i++)
			if (tree.getChild(i).toString().equals(name)) return i;

		return -1;
	}

	/**
	 * Find all nodes of a given type
	 *
	 * IMPORTANT: Nodes are return ALPHABETICALLY sorted
	 *
	 * @param clazz : Class to find (all nodes if null)
	 *
	 * @param recurse : If true, perform recursive search
	 *
	 * @param recurseInclude : If true, perform recursive search within 'StatementInclide' nodes.
	 *                         Note: If 'recurse' is set, the value of 'recurseInclude' is irrelevant
	 */
	@SuppressWarnings("rawtypes")
	public List<BdsNode> findNodes(Class clazz, boolean recurse, boolean recurseInclude) {
		HashSet<Object> visited = new HashSet<>();
		return findNodes(clazz, recurse, recurseInclude, visited);
	}

	@SuppressWarnings("rawtypes")
	protected List<BdsNode> findNodes(Class clazz, boolean recurse, boolean recurseInclude, Set<Object> visited) {
		List<BdsNode> list = new ArrayList<>();

		// Iterate over fields
		for (Field field : getAllClassFields()) {
			try {
				field.setAccessible(true);
				Object fieldObj = field.get(this);

				// Does the field have a map?
				if (fieldObj != null && !visited.contains(fieldObj)) {
					visited.add(fieldObj);

					// If it's an array, iterate on all objects
					if (fieldObj.getClass().isArray()) {
						for (Object fieldObjSingle : (Object[]) fieldObj)
							list.addAll(findNodes(clazz, fieldObjSingle, recurse, recurseInclude, visited));
					} else {
						list.addAll(findNodes(clazz, fieldObj, recurse, recurseInclude, visited));
					}

				}
			} catch (Exception e) {
				throw new RuntimeException("Error getting field '" + field.getName() + "' from class '" + this.getClass().getCanonicalName() + "'", e);
			}
		}

		return list;
	}

	/**
	 * Find all nodes of a given type
	 * @param clazz : If null, all nodes are added
	 * @param fieldObj
	 */
	@SuppressWarnings("rawtypes")
	List<BdsNode> findNodes(Class clazz, Object fieldObj, boolean recurse, boolean recurseInclude, Set<Object> visited) {
		List<BdsNode> list = new ArrayList<>();

		// If it is a BigDataScriptNode then we can recurse into it
		if ((fieldObj != null) && (fieldObj instanceof BdsNode)) {
			BdsNode bdsnode = ((BdsNode) fieldObj);

			// Found the requested type?
			if ((clazz == null) || (fieldObj.getClass() == clazz)) list.add((BdsNode) fieldObj);

			// Recurse into this field?
			if (recurse || (recurseInclude && bdsnode instanceof StatementInclude)) {
				list.addAll(bdsnode.findNodes(clazz, recurse, recurseInclude, visited));
			}
		}

		return list;
	}

	/**
	 * Find a parent of type 'clazz'
	 */
	@SuppressWarnings("rawtypes")
	protected BdsNode findParent(Class clazz) {
		if (this.getClass() == clazz) return this;
		if (parent != null) return parent.findParent(clazz);
		return null;
	}

	/**
	 * Find any parent node 'clazz' before any node 'stopAtClass'
	 */
	@SuppressWarnings("rawtypes")
	protected BdsNode findParent(Class clazz, Class stopAtClass) {
		if (this.getClass() == clazz) return this;
		if (this.getClass() == stopAtClass) return null;
		if (parent != null) return parent.findParent(clazz);
		return null;
	}

	@SuppressWarnings("rawtypes")
	protected BdsNode findParent(Set<Class> classSet) {
		if (classSet.contains(this.getClass())) return this;
		if (parent != null) return parent.findParent(classSet);
		return null;
	}

	List<Field> getAllClassFields() {
		return getAllClassFields(false, true, true, true, true, false, false);
	}

	List<Field> getAllClassFields(boolean addParent) {
		return getAllClassFields(addParent, true, true, false, true, false, false);
	}

	/**
	 * Get all fields from this class
	 *
	 * IMPORTANT: Nodes are returned ALPHABETICALLY sorted
	 */
	@SuppressWarnings("rawtypes")
	List<Field> getAllClassFields(boolean addParent, boolean addNode, boolean addPrimitive, boolean addClass, boolean addArray, boolean addStatic, boolean addPrivate) {
		// Top class (if we are looking for 'parent' field, we need to include BigDataScriptNode, otherwise we don't
		Class topClass = (addParent ? Object.class : BdsNode.class);

		// Get all fields for each parent class
		ArrayList<Field> fields = new ArrayList<>();

		for (Class clazz = this.getClass(); clazz != topClass; clazz = clazz.getSuperclass()) {
			for (Field f : clazz.getDeclaredFields()) {
				// Add field?
				if (Modifier.isPrivate(f.getModifiers())) {
					if (addPrivate) fields.add(f);
				} else if (Modifier.isStatic(f.getModifiers())) {
					if (addStatic) fields.add(f);
				} else if (f.getName().equals("parent")) {
					if (addParent) fields.add(f);
				} else if (f.getType().getCanonicalName().startsWith(BdsNodeFactory.get().packageName())) {
					if (addNode) fields.add(f);
				} else if (f.getType().isPrimitive() || (f.getType() == String.class)) {
					if (addPrimitive) fields.add(f);
				} else if (f.getType().isArray()) {
					if (addArray) fields.add(f);
				} else if (!f.getType().isPrimitive()) {
					if (addClass) fields.add(f);
				}
			}
		}

		// Sort by name
		Collections.sort(fields, new Comparator<Field>() {
			@Override
			public int compare(Field o1, Field o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		return fields;
	}

	public BdsThread getBigDataScriptThread() {
		if (parent != null) return parent.getBigDataScriptThread();
		return null;
	}

	public int getCharPosInLine() {
		return charPosInLine;
	}

	/**
	 * Find file (this information is stored in 'ProgramUnit', or in 'Block' node)
	 */
	public File getFile() {
		return parent != null ? parent.getFile() : null;
	}

	public String getFileName() {
		File f = getFile();
		return f == null ? null : f.toString();
	}

	public String getFileNameCanonical() {
		File f = getFile();

		try {
			return f == null ? null : f.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getId() {
		return id;
	}

	public int getLineNum() {
		return lineNum;
	}

	public BdsNode getParent() {
		return parent;
	}

	/**
	 * Recurse unti top node is found.
	 * Top node is always a 'ProgramUnit' node.
	 *
	 * @return
	 */
	public ProgramUnit getProgramUnit() {
		BdsNode n = this;
		while (n.getParent() != null)
			n = n.getParent();
		return (ProgramUnit) n;
	}

	/**
	 * Which type does this expression return?
	 */
	public Type getReturnType() {
		return returnType;
	}

	public SymbolTable getSymbolTable() {
		return null;
	}

	/**
	 * Find a child terminal node having 'str' as text
	 */
	protected int indexOf(ParseTree tree, String str) {
		for (int i = 0; i < tree.getChildCount(); i++)
			if (isTerminal(tree, i, str)) return i;

		return -1;
	}

	/**
	 * Initialize some defaults (before parsing)
	 */
	protected void initialize() {
	}

	public boolean isAny() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.ANY;
	}

	public boolean isBool() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.BOOL;
	}

	public boolean isClass() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.CLASS;
	}

	public boolean isFunction() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.FUNCTION;
	}

	public boolean isInt() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.INT;
	}

	public boolean isList() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.LIST;
	}

	public boolean isList(Type elementType) {
		if (isList()) {

			Type re = ((TypeList) returnType).getElementType();

			// If elementType is void, then the list must be empty
			// An empty list complies with all types
			if (re.isVoid() || re.isAny() || elementType.isAny()) return true;

			// Same element types?
			return re.equals(elementType);
		}
		return false;
	}

	public boolean isMap() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.MAP;
	}

	public boolean isMap(Type keyType, Type valueType) {
		if (isMap()) {
			TypeMap typeMap = (TypeMap) returnType;
			return typeMap.getKeyType().is(keyType) && typeMap.getValueType().is(valueType);
		}
		return false;
	}

	/**
	 * Does this node require a new scope
	 */
	public boolean isNeedsScope() {
		return false;
	}

	public boolean isNull() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.NULL;
	}

	public boolean isNumeric() {
		return isInt() || isReal();
	}

	public boolean isReal() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.REAL;
	}

	public boolean isReturnTypesNotNull() {
		return returnType != null;
	}

	/**
	 * Should we stop in this node when debugging?
	 */
	public boolean isStopDebug() {
		return true;
	}

	public boolean isString() {
		return returnType != null && returnType.getPrimitiveType() == PrimitiveType.STRING;
	}

	/**
	 * Is child 'idx' a terminal node with map 'str'?
	 */
	protected boolean isTerminal(ParseTree tree, int idx, String str) {
		ParseTree node = tree.getChild(idx);
		return ((node instanceof TerminalNode) && node.getText().equals(str));
	}

	public boolean isVoid() {
		return returnType.is(Types.VOID);
	}

	/**
	 * Try to get line number and character position
	 *
	 * Note: For some weird reason this info is only available at Token level.
	 *
	 * @param tree
	 */
	protected boolean lineAndPos(ParseTree tree) {
		// Is this a token?
		if (tree.getPayload() instanceof Token) {
			lineAndPos((Token) tree.getPayload());
			return true;
		}

		// Any child a token?
		for (int i = 0; i < tree.getChildCount(); i++) {
			ParseTree node = tree.getChild(i);
			if (node.getPayload() instanceof Token) {
				lineAndPos((Token) node.getPayload());
				return true;
			}
		}

		// Last resort: Recurse
		for (int i = 0; i < tree.getChildCount(); i++)
			if (lineAndPos(tree.getChild(i))) return true;

		// Cannot set
		return false;
	}

	void lineAndPos(Token token) {
		lineNum = token.getLine();
		charPosInLine = token.getCharPositionInLine();
	}

	/**
	 * Log a message to console
	 */
	public void log(String msg) {
		Timer.showStdErr(getClass().getSimpleName() //
				+ (getFileName() != null ? " (" + getFileName() + ":" + getLineNum() + ")" : "") //
				+ " : " + msg //
		);
	}

	/**
	 * Parse a tree
	 */
	protected abstract void parse(ParseTree tree);

	/**
	 * Show a parseTree node
	 */
	void printNode(ParseTree tree) {
		System.out.println(tree.getClass().getSimpleName());
		for (int i = 0; i < tree.getChildCount(); i++) {
			ParseTree node = tree.getChild(i);
			System.out.println("\tchild[" + i + "]\ttype: " + node.getClass().getSimpleName() + "\ttext: '" + node.getText() + "'");
		}
	}

	/**
	 * Calculate return type and assign it to 'returnType' variable.
	 */
	public Type returnType(SymbolTable symtab) {
		if (returnType != null) return returnType;
		returnType = Types.VOID;
		return returnType;
	}

	/**
	 * Perform several basic sanity checks right after parsing the tree
	 */
	public void sanityCheck(CompilerMessages compilerMessages) {
		// Default : Do nothing
	}

	public void setNeedsScope(boolean needsScope) {
		throw new RuntimeException("Cannot set 'needsScope' in this node:" + this.getClass().getSimpleName());
	}

	public void setParent(BdsNode parent) {
		this.parent = parent;
	}

	public void setSymbolTable(SymbolTable symtab) {
		throw new RuntimeException("Cannot set symbol table to node " + this.getClass().getSimpleName());
	}

	public String toAsm() {
		return toAsmNode();
	}

	public String toAsmNode() {
		// Show file, line and position if available
		if (getFileName() == null) return "node " + id;
		return "# " + getFileName() //
				+ (lineNum >= 0 ? ", line " + lineNum : "") //
				+ (charPosInLine >= 0 ? ", pos " + charPosInLine : "") //
				+ ", node: " + getClass().getSimpleName() //
				+ "\n" //
				+ "node " + id //
				+ "\n" //
		;
	}

	public String toAsmRetType() {
		if (isInt()) return "i";
		if (isReal()) return "r";
		if (isString()) return "s";
		if (isBool()) return "b";
		if (isList()) return "l";
		throw new RuntimeException();
	}

	@Override
	public String toString() {
		return "Program: " + getFileName() + "\n";
	}

	/**
	 * Show the tree
	 */
	public String toStringTree(String tabs, String fieldName) {
		StringBuilder out = new StringBuilder();

		// Not an array: Single field. Show
		out.append(tabs + this.getClass().getSimpleName() + " " + fieldName + "\t[" + id + " | " + (parent != null ? parent.getId() : "") + "]\n");

		// Iterate over fields
		for (Field field : getAllClassFields()) {
			try {
				field.setAccessible(true);
				Object fieldObj = field.get(this);

				// Does the field have a map?
				if (fieldObj != null) {

					// If it's an array, iterate on all objects
					if (fieldObj.getClass().isArray()) {
						int idx = 0;
						for (Object fieldObjSingle : (Object[]) fieldObj) {
							// We can recurse into this field
							if ((fieldObjSingle != null) && (fieldObjSingle instanceof BdsNode)) {
								BdsNode csnode = (BdsNode) fieldObjSingle;
								out.append(csnode.toStringTree(tabs + "\t", field.getName() + "[" + idx + "]") + "\n");
							}
							idx++;
						}
					} else {
						// We can recurse into this field
						if ((fieldObj != null) && (fieldObj instanceof BdsNode)) {
							BdsNode csnode = (BdsNode) fieldObj;
							out.append(csnode.toStringTree(tabs + "\t", field.getName()) + "\n");
						} else out.append(tabs + field.getType().getSimpleName() + " " + field.getName() + " : " + fieldObj + "\n");
					}
				} else {
					// Value of this field is null
					out.append(tabs + field.getType().getSimpleName() + " " + field.getName() + " = null\n");
				}
			} catch (Exception e) {
				throw new RuntimeException("Error getting field '" + field.getName() + "' from class '" + this.getClass().getCanonicalName() + "'", e);
			}
		}

		while ((out.length() > 2) && (out.charAt(out.length() - 1) == '\n') && (out.charAt(out.length() - 2) == '\n'))
			out.deleteCharAt(out.length() - 1);

		return out.toString();
	}

	/**
	 * Type checking (compilation step)
	 * @param scope
	 * @param compilerMessages
	 */
	public void typeCheck(SymbolTable symtab, CompilerMessages compilerMessages) {
		// Calculate return type
		returnType = returnType(symtab);

		// Are return types non-null?
		// Note: null returnTypes happen if variables are missing.
		if (isReturnTypesNotNull()) typeCheckNotNull(symtab, compilerMessages);
	}

	/**
	 * Perform several basic type checking tasks.
	 * Invoke 'typeCheck()' method on all sub-nodes
	 */
	public void typeChecking(SymbolTable symtab, CompilerMessages compilerMessages) {
		// Create a new scope?
		boolean newSymTab = false;
		if (isNeedsScope()) {
			SymbolTable newSymtab = new SymbolTable(this);
			symtab = newSymtab;
			setSymbolTable(newSymtab);
			newSymTab = true;;
		}

		// Once the scope is right, we can perform the real type-check
		typeCheck(symtab, compilerMessages);

		// Get all sub-nodes (first level, do not recurse)
		List<BdsNode> nodes = findNodes(null, false, false);

		// Add this node as 'type-checked' to avoid infinite recursion
		TypeCheckedNodes.get().add(this);

		// Check all sub-nodes
		for (BdsNode node : nodes) {
			// Not already type-checked? Go ahead
			if (!TypeCheckedNodes.get().isTypeChecked(node)) {
				node.typeChecking(symtab, compilerMessages);
			}
		}

		// Restore old SymbolTable?
		if (newSymTab) {
			// Do we really need a SymbolTable?
			// If SymbolTable is empty, we don't really need it
			if (symtab.isEmpty()) {
				setNeedsScope(false);
				setSymbolTable(null);
			} else setSymbolTable(symtab);

			// Restore previous SymbolTable
			symtab = symtab.getParent();
		}
	}

	/**
	 * Type checking.
	 * This is invoked once we made sure all return types are non null (so we don't have to check for null every time)
	 */
	protected void typeCheckNotNull(SymbolTable symtab, CompilerMessages compilerMessages) {
		// Nothing to do
	}

	/**
	 * Update ID field
	 */
	protected void updateId(int newId) {
		BdsNodeFactory.get().updateId(id, newId, this);
		id = newId;
	}
}
