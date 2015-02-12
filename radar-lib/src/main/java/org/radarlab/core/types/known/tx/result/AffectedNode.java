package org.radarlab.core.types.known.tx.result;

import org.radarlab.core.STObject;
import org.radarlab.core.fields.Field;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.serialized.SerializedType;
import org.radarlab.core.serialized.enums.LedgerEntryType;

public class AffectedNode extends STObject {
    Field field;
    STObject nested;

    public AffectedNode(STObject source) {
        fields = source.getFields();
        field = getField();
        nested = getNested();
    }

    public boolean wasPreviousNode() {
        return isDeletedNode() || isModifiedNode();
    }

    public boolean isCreatedNode() {
        return field == Field.CreatedNode;
    }

    public boolean isDeletedNode() {
        return field == Field.DeletedNode;
    }

    public boolean isModifiedNode() {
        return field == Field.ModifiedNode;
    }

    public Field getField() {
        return fields.firstKey();
    }

    public Hash256 ledgerIndex() {
        return nested.get(Hash256.LedgerIndex);
    }

    public LedgerEntryType ledgerEntryType() {
        return ledgerEntryType(nested);
    }

    private STObject getNested() {
        return (STObject) get(getField());
    }

    public STObject nodeAsPrevious() {
        return rebuildFromMeta(true);
    }

    public STObject nodeAsFinal() {
        return rebuildFromMeta(false);
    }

    public STObject rebuildFromMeta(boolean layerPrevious) {
        STObject mixed = new STObject();
        boolean created = isCreatedNode();

        Field wrapperField = created ? Field.CreatedNode :
                isDeletedNode() ? Field.DeletedNode :
                        Field.ModifiedNode;

        STObject wrapped = (STObject) get(wrapperField);

        Field finalFields = created ? Field.NewFields :
                Field.FinalFields;

        if (!wrapped.has(finalFields)) {
            return STObject.formatted(new STObject(wrapped.getFields()));
        }

        STObject finals = (STObject) wrapped.get(finalFields);
        for (Field field : finals) {
            mixed.put(field, finals.get(field));
        }

        // DirectoryNode LedgerEntryType won't have `PreviousFields`
        if (layerPrevious && wrapped.has(Field.PreviousFields)) {
            STObject previous = wrapped.get(STObject.PreviousFields);
            STObject changed = new STObject();
            mixed.put(Field.FinalFields, changed);

            for (Field field : previous) {
                mixed.put(field, previous.get(field));
                changed.put(field, finals.get(field));
            }
        }

        for (Field field : wrapped) {
            switch (field) {
                case NewFields:
                case PreviousFields:
                case FinalFields:
                    continue;
                default:
                    SerializedType value = wrapped.get(field);

                    if (field == Field.LedgerIndex) {
                        field = Field.index;
                    }
                    mixed.put(field, value);

            }
        }
        return STObject.formatted(mixed);
    }

    public static boolean isAffectedNode(STObject source) {
        return (source.size() == 1 && (
                source.has(DeletedNode) ||
                source.has(CreatedNode) ||
                source.has(ModifiedNode)));
    }
}
