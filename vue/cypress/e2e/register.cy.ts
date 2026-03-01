describe('Register page', () => {
  beforeEach(() => {
    cy.intercept('POST', '**/auth/register', {
      token: 'fake-jwt',
      user: { id: 2, fullName: 'New User', account: { id: 1 } }
    }).as('registerRequest')
    cy.visit('/register')
  })

  it('shows the registration form', () => {
    cy.get('#username').should('exist')
    cy.get('#email').should('exist')
    cy.get('#fullName').should('exist')
    cy.get('#password').should('exist')
    cy.get('#confirmPassword').should('exist')
  })

  it('displays password mismatch error', () => {
    cy.get('#password').type('pass1')
    cy.get('#confirmPassword').type('pass2')
    cy.contains('Passwords do not match').should('be.visible')
    cy.get('button[type=submit]').should('be.disabled')
  })

  it('can register successfully and redirect', () => {
    cy.get('#username').type('newuser')
    cy.get('#email').type('new@example.com')
    cy.get('#fullName').type('New User')
    cy.get('#password').type('secret')
    cy.get('#confirmPassword').type('secret')
    cy.get('button[type=submit]').click()
    cy.wait('@registerRequest')
    cy.url().should('include', '/account/profile')
  })
})