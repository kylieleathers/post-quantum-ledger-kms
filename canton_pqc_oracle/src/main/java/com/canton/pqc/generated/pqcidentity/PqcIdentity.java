package com.canton.pqc.generated.pqcidentity;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseByKeyCommand;
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
import com.daml.ledger.javaapi.data.codegen.ContractWithKey;
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
import com.daml.ledger.javaapi.data.codegen.json.JsonLfWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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

public final class PqcIdentity extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcIdentity", "PqcIdentity");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e", "PqcIdentity", "PqcIdentity");

  public static final String PACKAGE_ID = "fca1340399695b73cb4e49cb1fe1e63905c38744e776bd5d4885697ff9649c7e";

  public static final Choice<PqcIdentity, com.canton.pqc.generated.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.canton.pqc.generated.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithKey<Contract, ContractId, PqcIdentity, String> COMPANION = 
      new ContractCompanion.WithKey<>("com.canton.pqc.generated.pqcidentity.PqcIdentity",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> PqcIdentity.templateValueDecoder().decode(v), PqcIdentity::fromJson, Contract::new,
        List.of(CHOICE_Archive), e -> PrimitiveValueDecoders.fromParty.decode(e));

  public static final String PACKAGE_NAME = "canton-pqc-oracle";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String party;

  public final String publicKeyB64;

  public PqcIdentity(String party, String publicKeyB64) {
    this.party = party;
    this.publicKeyB64 = publicKeyB64;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(PqcIdentity.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseArchive} instead
   */
  @Deprecated
  public static Update<Exercised<Unit>> exerciseByKeyArchive(String key,
      com.canton.pqc.generated.da.internal.template.Archive arg) {
    return byKey(key).exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseArchive()} instead
   */
  @Deprecated
  public static Update<Exercised<Unit>> exerciseByKeyArchive(String key) {
    return byKey(key).exerciseArchive();
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

  public static Update<Created<ContractId>> create(String party, String publicKeyB64) {
    return new PqcIdentity(party, publicKeyB64).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithKey<Contract, ContractId, PqcIdentity, String> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static PqcIdentity fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<PqcIdentity> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(2);
    fields.add(new DamlRecord.Field("party", new Party(this.party)));
    fields.add(new DamlRecord.Field("publicKeyB64", new Text(this.publicKeyB64)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<PqcIdentity> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0, recordValue$);
      String party = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String publicKeyB64 = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      return new PqcIdentity(party, publicKeyB64);
    } ;
  }

  public static JsonLfDecoder<PqcIdentity> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("party", "publicKeyB64"), name -> {
          switch (name) {
            case "party": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "publicKeyB64": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new PqcIdentity(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static PqcIdentity fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("party", apply(JsonLfEncoders::party, party)),
        JsonLfEncoders.Field.of("publicKeyB64", apply(JsonLfEncoders::text, publicKeyB64)));
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
    if (!(object instanceof PqcIdentity)) {
      return false;
    }
    PqcIdentity other = (PqcIdentity) object;
    return Objects.equals(this.party, other.party) &&
        Objects.equals(this.publicKeyB64, other.publicKeyB64);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.party, this.publicKeyB64);
  }

  @Override
  public String toString() {
    return String.format("com.canton.pqc.generated.pqcidentity.PqcIdentity(%s, %s)", this.party,
        this.publicKeyB64);
  }

  /**
   * Set up an {@link ExerciseByKeyCommand}; invoke an {@code exercise} method on the result of
      this to finish creating the command, or convert to an interface first with {@code toInterface}
      to invoke an interface {@code exercise} method.
   */
  public static ByKey byKey(String key) {
    return new ByKey(new Party(key));
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<PqcIdentity> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcIdentity, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<PqcIdentity> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends ContractWithKey<ContractId, PqcIdentity, String> {
    public Contract(ContractId id, PqcIdentity data, Optional<String> agreementText,
        Optional<String> key, Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, key, signatories, observers);
    }

    public static JsonLfDecoder<String> keyJsonDecoder() {
      return JsonLfDecoders.party;
    }

    public static String keyFromJson(String json) throws JsonLfDecoder.Error {
      return keyJsonDecoder().decode(new JsonLfReader(json));
    }

    public JsonLfEncoder keyJsonEncoder() {
      return this.key.map(JsonLfEncoders::party).orElse(null);
    }

    public String keyToJson() {
      var enc = keyJsonEncoder();
      if (enc == null) return null;
      var w = new StringWriter();
      try {
        enc.encode(new JsonLfWriter(w));
      } catch (IOException e) {
        // Not expected with StringWriter
        throw new UncheckedIOException(e);
      }
      return w.toString();
    }

    @Override
    protected ContractCompanion<Contract, ContractId, PqcIdentity> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Optional<String> agreementText, Optional<String> key, Set<String> signatories,
        Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, agreementText, key, signatories,
          observers);
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
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcIdentity, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PqcIdentity> get() {
      return jsonDecoder();
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey implements Exercises<ExerciseByKeyCommand> {
    ByKey(Value key) {
      super(key);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PqcIdentity, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }
}
