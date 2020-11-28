package taintAnalysis.utility;

import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.Host;
import soot.tagkit.Tag;
import soot.util.Switch;

import java.util.List;
import java.util.Objects;

public class PhantomRetStmt implements Stmt {

    private final SootMethod method;

    public static PhantomRetStmt getInstance(SootMethod method) {
        return new PhantomRetStmt(method);
    }

    private PhantomRetStmt(SootMethod method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "return";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhantomRetStmt that = (PhantomRetStmt) o;
        return Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }

    @Override
    public List<ValueBox> getUseBoxes() {
        return null;
    }

    @Override
    public List<ValueBox> getDefBoxes() {
        return null;
    }

    @Override
    public List<UnitBox> getUnitBoxes() {
        return null;
    }

    @Override
    public List<UnitBox> getBoxesPointingToThis() {
        return null;
    }

    @Override
    public void addBoxPointingToThis(UnitBox b) {

    }

    @Override
    public void removeBoxPointingToThis(UnitBox b) {

    }

    @Override
    public void clearUnitBoxes() {

    }

    @Override
    public List<ValueBox> getUseAndDefBoxes() {
        return null;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public boolean fallsThrough() {
        return false;
    }

    @Override
    public boolean branches() {
        return false;
    }

    @Override
    public void toString(UnitPrinter up) {

    }

    @Override
    public void redirectJumpsToThisTo(Unit newLocation) {

    }

    @Override
    public boolean containsInvokeExpr() {
        return false;
    }

    @Override
    public InvokeExpr getInvokeExpr() {
        return null;
    }

    @Override
    public ValueBox getInvokeExprBox() {
        return null;
    }

    @Override
    public boolean containsArrayRef() {
        return false;
    }

    @Override
    public ArrayRef getArrayRef() {
        return null;
    }

    @Override
    public ValueBox getArrayRefBox() {
        return null;
    }

    @Override
    public boolean containsFieldRef() {
        return false;
    }

    @Override
    public FieldRef getFieldRef() {
        return null;
    }

    @Override
    public ValueBox getFieldRefBox() {
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return null;
    }

    @Override
    public Tag getTag(String aName) {
        return null;
    }

    @Override
    public void addTag(Tag t) {

    }

    @Override
    public void removeTag(String name) {

    }

    @Override
    public boolean hasTag(String aName) {
        return false;
    }

    @Override
    public void removeAllTags() {

    }

    @Override
    public void addAllTagsOf(Host h) {

    }

    @Override
    public int getJavaSourceStartLineNumber() {
        return 0;
    }

    @Override
    public int getJavaSourceStartColumnNumber() {
        return 0;
    }

    @Override
    public void apply(Switch sw) {

    }

}
