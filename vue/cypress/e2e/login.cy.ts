describe('Login page', () => {
  beforeEach(() => {
    // intercept both login success and failure using glob so path matches /api/auth/login
    cy.intercept('POST', '**/auth/login', (req) => {
      if (req.body && req.body.username === 'bad') {
        req.reply({ statusCode: 401, body: { message: 'invalid' } })
      } else {
        req.reply({ body: { token: 'fake-jwt', user: { id: 1, fullName: 'Test User', account: { id: 1 } } } })
      }
    }).as('loginRequest')
    cy.visit('/login')
  })

  it('displays the login form', () => {
    cy.get('#username').should('exist')
    cy.get('#password').should('exist')
    cy.get('button[type=submit]').contains('Login')
  })

  it('toggles password visibility', () => {
    cy.get('#password').type('secret')
    cy.get('button.password-toggle').click()
    cy.get('#password').should('have.attr', 'type', 'text')
    cy.get('button.password-toggle').click()
    cy.get('#password').should('have.attr', 'type', 'password')
  })

  it('opens forgot password modal', () => {
    // force an error so the link becomes visible
    cy.get('#username').type('bad')
    cy.get('#password').type('wrong')
    cy.get('button[type=submit]').click()
    cy.wait('@loginRequest')
    cy.get('.forgot-password-link button').click()
    cy.get('.modal-overlay').should('be.visible')
    cy.get('input#resetEmail').should('exist')
  })

  it('logs in successfully and redirects', () => {
    cy.get('#username').type('user1')
    cy.get('#password').type('password1')
    cy.get('button[type=submit]').click()
    cy.wait('@loginRequest')
    cy.url().should('include', '/account/profile')
  })
})