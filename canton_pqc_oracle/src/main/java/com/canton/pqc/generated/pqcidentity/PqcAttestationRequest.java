package com.canton.pqc.generated.pqcidentity;

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

public final class PqcAttestationRequest extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcIdentity", "PqcAttestationRequest");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcIdentity", "PqcAttestationRequest");

  public static final String PACKAGE_ID = "fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e";

  public static final Choice<PqcAttestationRequest, com.canton.pqc.generated.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.canton.pqc.generated.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<PqcAttestationRequest, Reject, Unit> CHOICE_Reject = 
      Choice.create("Reject", value$ -> value$.toValue(), value$ -> Reject.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<PqcAttestationRequest, Accept, PqcAttestation.ContractId> CHOICE_Accept = 
      Choice.create("Accept", value$ -> value$.toValue(), value$ -> Accept.valueDecoder()
        .decode(value$), value$ ->
        new PqcAttestation.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, PqcAttestationRequest> COMPANION = 
      new ContractCompanion.WithoutKey<>(
        "com.canton.pqc.generated.pqcidentity.PqcAttestationRequest", TEMPLATE_ID,
        TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> PqcAttestationRequest.templateValueDecoder().decode(v),
        PqcAttestationRequest::fromJson, Contract::new, List.of(CHOICE_Archive, CHOICE_Reject,
        CHOICE_Accept));

  public static final String PACKAGE_NAME = "canton-pqc-oracle";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String party;

  public final String oracle;

  public final String payloadHashB64;

  public final String signatureB64;

  public PqcAttestationRequest(String party, String oracle, String payloadHashB64,
      String signatureB64) {
    this.party = party;
    this.oracle = oracle;
    this.payloadHashB64 = payloadHashB64;
    this.signatureB64 = signatureB64;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(PqcAttestationRequest.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReject} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseReject(Reject arg) {
    return createAnd().exerciseReject(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReject} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseReject() {
    return createAndExerciseReject(new Reject());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAccept} instead
   */
  @Deprecated
  public Update<Exercised<PqcAttestation.ContractId>> createAndExerciseAccept(Accept arg) {
    return createAnd().exerciseAccept(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAccept} instead
   */
  @Deprecated
  public Update<Exercised<PqcAttestation.ContractId>> createAndExerciseAccept() {
    return createAndExerciseAccept(new Accept());
  }

  public static Update<Created<ContractId>> create(String party, String oracle,
      String payloadHashB64, String signatureB64) {
    return new PqcAttestationRequest(party, oracle, payloadHashB64, signatureB64).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, PqcAttestationRequest> getCompanion(
      ) {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static PqcAttestationRequest fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<PqcAttestationRequest> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(4);
    fields.add(new DamlRecord.Field("party", new Party(this.party)));
    fields.add(new DamlRecord.Field("oracle", new Party(this.oracle)));
    fields.add(new DamlRecord.Field("payloadHashB64", new Text(this.payloadHashB64)));
    fields.add(new DamlRecord.Field("signatureB64", new Text(this.signatureB64)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<PqcAttestationRequest> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0, recordValue$);
      String party = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String oracle = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String payloadHashB64 = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String signatureB64 = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      return new PqcAttestationRequest(party, oracle, payloadHashB64, signatureB64);
    } ;
  }

  public static JsonLfDecoder<PqcAttestationRequest> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("party", "oracle", "payloadHashB64", "signatureB64"), name -> {
          switch (name) {
            case "party": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "oracle": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "payloadHashB64": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "signatureB64": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new PqcAttestationRequest(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static PqcAttestationRequest fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("party", apply(JsonLfEncoders::party, party)),
        JsonLfEncoders.Field.of("oracle", apply(JsonLfEncoders::party, oracle)),
        JsonLfEncoders.Field.of("payloadHashB64", apply(JsonLfEncoders::text, payloadHashB64)),
        JsonLfEncoders.Field.of("signatureB64", apply(JsonLfEncoders::text, signatureB64)));
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
    if (!(object instanceof PqcAttestationRequest)) {
      return false;
    }
    PqcAttestationRequest other = (PqcAttestationRequest) object;
    return Objects.equals(this.party, other.party) && Objects.equals(this.oracle, other.oracle) &&
        Objects.equals(this.payloadHashB64, other.payloadHashB64) &&
        Objects.equals(this.signatureB64, other.signatureB64);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.party, this.oracle, this.payloadHashB64, this.signatureB64);
  }

  @Override
  public String toString() {
    return String.format("com.canton.pqc.generated.pqcidentity.PqcAttestationRequest(%s, %s, %s, %s)",
        this.party, this.oracle, this.payloadHashB64, this.signatureB64);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<PqcAttestationRequest> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcAttestationRequest, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<PqcAttestationRequest> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, PqcAttestationRequest> {
    public Contract(ContractId id, PqcAttestationRequest data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, PqcAttestationRequest> getCompanion() {
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

    default Update<Exercised<Unit>> exerciseReject(Reject arg) {
      return makeExerciseCmd(CHOICE_Reject, arg);
    }

    default Update<Exercised<Unit>> exerciseReject() {
      return exerciseReject(new Reject());
    }

    default Update<Exercised<PqcAttestation.ContractId>> exerciseAccept(Accept arg) {
      return makeExerciseCmd(CHOICE_Accept, arg);
    }

    default Update<Exercised<PqcAttestation.ContractId>> exerciseAccept() {
      return exerciseAccept(new Accept());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcAttestationRequest, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PqcAttestationRequest> get() {
      return jsonDecoder();
    }
  }
}
