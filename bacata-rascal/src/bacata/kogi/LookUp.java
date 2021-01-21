package bacata.kogi;

import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.staticErrors.UndeclaredType;
import org.rascalmpl.types.TypeReifier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory;


public class LookUp {

	private final IValueFactory vf;

	public LookUp(IValueFactory vf) { 
		this.vf = vf;
	}

	public IConstructor lookupType(IString s, IEvaluatorContext ctx) {
		Environment currentEnvt = ctx.getCurrentEnvt();
		String name = s.getValue();
		io.usethesource.vallang.type.Type type = null;

		if ("int".equals(name)) {
			type = TypeFactory.getInstance().integerType();
		} 
		else if ("str".equals(name)) {
			type = TypeFactory.getInstance().stringType();
		} 
		else if (currentEnvt != null) {
			type = currentEnvt.lookupAlias(name);

			if (type == null) {
				type = currentEnvt.lookupConcreteSyntaxType(name);
			}

			if (type == null) {
				type = currentEnvt.lookupAbstractDataType(name);
			}
		}
		if (type != null) {
			return new TypeReifier(vf).typeToValue(type, currentEnvt.getStore(), vf.mapWriter().done());
		}
		throw new UndeclaredType(name, null);
	}
}
