package org.apache.isis.core.runtime.systemusinginstallers;

import org.apache.isis.core.runtime.authorization.standard.AuthorizationManagerStandard;
import org.apache.isis.core.security.authentication.manager.AuthenticationManager;
import org.apache.isis.core.security.authentication.standard.AuthenticationManagerStandard;
import org.apache.isis.core.security.authorization.manager.AuthorizationManager;

import static org.apache.isis.commons.internal.base._With.computeIfAbsent;

public class IsisComponentProviderBuilder {
    
    private AuthenticationManager authenticationManager;
    private AuthorizationManager authorizationManager;
    
    public IsisComponentProviderBuilder authenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        return this;
    }
    
    public IsisComponentProviderBuilder authorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
        return this;
    }
    
    // -- BUILD
    
    public IsisComponentProvider build() {
        
        authenticationManager = computeIfAbsent(authenticationManager, 
                IsisComponentProviderBuilder::authenticationManagerWithBypass);
        
        authorizationManager = computeIfAbsent(authorizationManager, 
                AuthorizationManagerStandard::new);
        
        return new IsisComponentProvider(authenticationManager, authorizationManager);
    }
    
    // -- HELPER
    
    
    /**
     * The standard authentication manager, configured with the 'bypass' authenticator 
     * (allows all requests through).
     * <p>
     * integration tests ignore appManifest for authentication and authorization.
     */
    private static AuthenticationManager authenticationManagerWithBypass() {
        final AuthenticationManagerStandard authenticationManager = new AuthenticationManagerStandard();
        authenticationManager.addAuthenticator(new AuthenticatorBypass());
        return authenticationManager;
    }
    
    
}
