describe('Account pages (authenticated)', () => {
  beforeEach(() => {
    // set fake auth token in localStorage so router doesn't redirect
    cy.window().then(win => {
      win.localStorage.setItem('jwt_token', 'fake');
      win.localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Cypress', role: 'ADMIN', account: { id: 1 } }));
    })
    // stub some basic account API calls if they occur
    cy.intercept('GET', '/accounts/1', { id: 1, name: 'Test Account' });
    cy.visit('/account/profile')
  })

  it('shows account navigation links', () => {
    cy.contains('Profile').should('exist')
    cy.contains('Users').should('exist')
    cy.contains('Data').should('exist')
    cy.contains('Waitlist').should('exist')
    cy.contains('Messages').should('exist')
  })

  it('can navigate to the users tab', () => {
    cy.contains('Users').click()
    cy.url().should('include', '/account/users')
  })

  it('can navigate to the data tab', () => {
    cy.contains('Data').click()
    cy.url().should('include', '/account/data')
  })

  it('exposes a link to the waitlist tab', () => {
    // ensure the link has correct href (we don't always need to click it)
    cy.contains('a.nav-button', 'Waitlist')
      .should('have.attr', 'href')
      .and('include', '/account/waitlist')
  })
})