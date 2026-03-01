describe('Reset Password page', () => {
  beforeEach(() => {
    cy.intercept('POST', '**/auth/reset-password', {
      message: 'Password changed'
    }).as('resetRequest')
    // supply token/email params so form is shown
    cy.visit('/reset-password?token=abc&email=test@ex.com')
  })

  it('displays the reset form', () => {
    cy.get('#newPassword').should('exist')
    cy.get('#confirmPassword').should('exist')
    cy.get('button[type=submit]').contains('Reset Password')
  })

  it('submits the form successfully', () => {
    cy.get('#newPassword').type('newpass')
    cy.get('#confirmPassword').type('newpass')
    cy.get('button[type=submit]').click()
    cy.wait('@resetRequest')
    cy.contains('Password changed').should('be.visible')
  })
})