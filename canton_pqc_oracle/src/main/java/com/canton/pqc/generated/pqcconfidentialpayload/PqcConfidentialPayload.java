package com.canton.pqc.generated.pqcconfidentialpayload;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.Choice;
import com.daml.ledger.javaapi.data.codegen.ContractCompanion;
import com.daml.ledger.javaapi.data.codegen.ContractTypeCompanion;
import com.daml.ledger.javaapi.data.codegen.Created;
import com.daml.ledger.javaapi.data.codegen.Exercised;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.Update;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PqcConfidentialPayload extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcConfidentialPayload", "PqcConfidentialPayload");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcConfidentialPayload", "PqcConfidentialPayload");

  public static final String PACKAGE_ID = "fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e";

  public static final Choice<PqcConfidentialPayload, com.canton.pqc.generated.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.canton.pqc.generated.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<PqcConfidentialPayload, Acknowledge, Unit> CHOICE_Acknowledge = 
      Choice.create("Acknowledge", value$ -> value$.toValue(), value$ -> Acknowledge.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, PqcConfidentialPayload> COMPANION = 
      new ContractCompanion.WithoutKey<>(
        "com.canton.pqc.generated.pqcconfidentialpayload.PqcConfidentialPayload", TEMPLATE_ID,
        TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> PqcConfidentialPayload.templateValueDecoder().decode(v),
        PqcConfidentialPayload::fromJson, Contract::new, List.of(CHOICE_Archive,
        CHOICE_Acknowledge));

  public static final String PACKAGE_NAME = "canton-pqc-oracle";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String sender;

  public final String recipient;

  public final String ephemeralXPubHex;

  public final String kemEncapsulationHex;

  public final String nonceHex;

  public final String aesCiphertextHex;

  public PqcConfidentialPayload(String sender, String recipient, String ephemeralXPubHex,
      String kemEncapsulationHex, String nonceHex, String aesCiphertextHex) {
    this.sender = sender;
    this.recipient = recipient;
    this.ephemeralXPubHex = ephemeralXPubHex;
    this.kemEncapsulationHex = kemEncapsulationHex;
    this.nonceHex = nonceHex;
    this.aesCiphertextHex = aesCiphertextHex;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(PqcConfidentialPayload.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive(
      com.canton.pqc.generated.da.internal.template.Archive arg) {
    return createAnd().exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive() {
    return createAndExerciseArchive(new com.canton.pqc.generated.da.internal.template.Archive());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAcknowledge} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseAcknowledge(Acknowledge arg) {
    return createAnd().exerciseAcknowledge(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAcknowledge} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseAcknowledge() {
    return createAndExerciseAcknowledge(new Acknowledge());
  }

  public static Update<Created<ContractId>> create(String sender, String recipient,
      String ephemeralXPubHex, String kemEncapsulationHex, String nonceHex,
      String aesCiphertextHex) {
    return new PqcConfidentialPayload(sender, recipient, ephemeralXPubHex, kemEncapsulationHex,
        nonceHex, aesCiphertextHex).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, PqcConfidentialPayload> getCompanion(
      ) {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static PqcConfidentialPayload fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<PqcConfidentialPayload> valueDecoder() throws
      IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(6);
    fields.add(new DamlRecord.Field("sender", new Party(this.sender)));
    fields.add(new DamlRecord.Field("recipient", new Party(this.recipient)));
    fields.add(new DamlRecord.Field("ephemeralXPubHex", new Text(this.ephemeralXPubHex)));
    fields.add(new DamlRecord.Field("kemEncapsulationHex", new Text(this.kemEncapsulationHex)));
    fields.add(new DamlRecord.Field("nonceHex", new Text(this.nonceHex)));
    fields.add(new DamlRecord.Field("aesCiphertextHex", new Text(this.aesCiphertextHex)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<PqcConfidentialPayload> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0, recordValue$);
      String sender = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String recipient = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String ephemeralXPubHex = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String kemEncapsulationHex = PrimitiveValueDecoders.fromText
          .decode(fields$.get(3).getValue());
      String nonceHex = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String aesCiphertextHex = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      return new PqcConfidentialPayload(sender, recipient, ephemeralXPubHex, kemEncapsulationHex,
          nonceHex, aesCiphertextHex);
    } ;
  }

  public static JsonLfDecoder<PqcConfidentialPayload> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("sender", "recipient", "ephemeralXPubHex", "kemEncapsulationHex", "nonceHex", "aesCiphertextHex"), name -> {
          switch (name) {
            case "sender": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "recipient": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "ephemeralXPubHex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "kemEncapsulationHex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "nonceHex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "aesCiphertextHex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new PqcConfidentialPayload(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static PqcConfidentialPayload fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("sender", apply(JsonLfEncoders::party, sender)),
        JsonLfEncoders.Field.of("recipient", apply(JsonLfEncoders::party, recipient)),
        JsonLfEncoders.Field.of("ephemeralXPubHex", apply(JsonLfEncoders::text, ephemeralXPubHex)),
        JsonLfEncoders.Field.of("kemEncapsulationHex", apply(JsonLfEncoders::text, kemEncapsulationHex)),
        JsonLfEncoders.Field.of("nonceHex", apply(JsonLfEncoders::text, nonceHex)),
        JsonLfEncoders.Field.of("aesCiphertextHex", apply(JsonLfEncoders::text, aesCiphertextHex)));
  }

  public static ContractFilter<Contract> contractFilter() {
    return ContractFilter.of(COMPANION);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PqcConfidentialPayload)) {
      return false;
    }
    PqcConfidentialPayload other = (PqcConfidentialPayload) object;
    return Objects.equals(this.sender, other.sender) &&
        Objects.equals(this.recipient, other.recipient) &&
        Objects.equals(this.ephemeralXPubHex, other.ephemeralXPubHex) &&
        Objects.equals(this.kemEncapsulationHex, other.kemEncapsulationHex) &&
        Objects.equals(this.nonceHex, other.nonceHex) &&
        Objects.equals(this.aesCiphertextHex, other.aesCiphertextHex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sender, this.recipient, this.ephemeralXPubHex,
        this.kemEncapsulationHex, this.nonceHex, this.aesCiphertextHex);
  }

  @Override
  public String toString() {
    return String.format("com.canton.pqc.generated.pqcconfidentialpayload.PqcConfidentialPayload(%s, %s, %s, %s, %s, %s)",
        this.sender, this.recipient, this.ephemeralXPubHex, this.kemEncapsulationHex, this.nonceHex,
        this.aesCiphertextHex);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<PqcConfidentialPayload> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcConfidentialPayload, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<PqcConfidentialPayload> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, PqcConfidentialPayload> {
    public Contract(ContractId id, PqcConfidentialPayload data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, PqcConfidentialPayload> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Optional<String> agreementText, Set<String> signatories, Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, agreementText, signatories, observers);
    }

    public static Contract fromCreatedEvent(CreatedEvent event) {
      return COMPANION.fromCreatedEvent(event);
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archive<Cmd> {
    default Update<Exercised<Unit>> exerciseArchive(
        com.canton.pqc.generated.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.canton.pqc.generated.da.internal.template.Archive());
    }

    default Update<Exercised<Unit>> exerciseAcknowledge(Acknowledge arg) {
      return makeExerciseCmd(CHOICE_Acknowledge, arg);
    }

    default Update<Exercised<Unit>> exerciseAcknowledge() {
      return exerciseAcknowledge(new Acknowledge());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcConfidentialPayload, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PqcConfidentialPayload> get() {
      return jsonDecoder();
    }
  }
}
