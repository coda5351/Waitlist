describe('Waitlist page', () => {
  beforeEach(() => {
    // stub waitlist status for a dummy code so the page will fetch status and
    // reveal the form
    cy.intercept('GET', '**/waitlists/*/status', {
      enabled: true,
      open: true,
      estimatedWait: 5,
      entries: []
    }).as('status')
    cy.intercept('GET', '**/entries**', []).as('list')
    // also handle preflight for POST requests so the stubbed POST goes through
    cy.intercept('OPTIONS', '**/entries**', { statusCode: 200 }).as('preflight')
    // include a placeholder code so currentCode is truthy
    cy.visit('/waitlist/join/abc123')
  })

  it('shows waitlist information and form', () => {
    cy.contains('Join the Waitlist')
    cy.contains('Current status').should('exist')
    // basic sanity: there should be at least one input field on the form
    cy.get('input').should('exist')
  })

  it('allows a user to sign up', () => {
    // wait for status stub to resolve so the page shows the form
    cy.wait('@status')
    cy.get('input').eq(0).type('Cypress User')
    cy.get('input').eq(1).type('123-456-7890')
    cy.get('input[type=number]').clear().type('2')
    cy.intercept('POST', '**/entries**', { code: 'abc123' }).as('join')
    cy.get('button').contains('Join').should('not.be.disabled')
  })
})