import { useState } from 'react';
import styles from './ConsentForm.module.css';

export default function ConsentForm({ language, onSubmit, loading }) {
  const [form, setForm] = useState({
    clientName: '',
    clientDob: '',
    legalName: '',
    relationship: '',
    agreed: false,
  });

  const isHindi = language === 'hi';
  const labels = {
    title: isHindi ? 'सूचित सहमति' : 'Informed Consent',
    clientName: isHindi ? 'बच्चे का नाम' : "Child's Name",
    clientDob: isHindi ? 'जन्म तिथि' : 'Date of Birth',
    legalName: isHindi ? 'अभिभावक का नाम' : "Guardian's Name",
    relationship: isHindi ? 'रिश्ता' : 'Relationship',
    agree: isHindi
      ? 'मैं इस मूल्यांकन के लिए अपनी सहमति देता/देती हूँ'
      : 'I consent to this assessment being conducted',
    submit: isHindi ? 'जमा करें' : 'Submit Consent',
    disclaimer: isHindi
      ? 'आपके डेटा को सुरक्षित रूप से संग्रहीत किया जाएगा और केवल मूल्यांकन उद्देश्यों के लिए उपयोग किया जाएगा।'
      : 'Your data will be stored securely and used only for assessment purposes.',
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.agreed) return;
    onSubmit(form);
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h2 className={styles.title}>{labels.title}</h2>

      <label className={styles.label}>
        {labels.clientName}
        <input name="clientName" className={styles.input} value={form.clientName} onChange={handleChange} required />
      </label>

      <label className={styles.label}>
        {labels.clientDob}
        <input name="clientDob" type="date" className={styles.input} value={form.clientDob} onChange={handleChange} required />
      </label>

      <label className={styles.label}>
        {labels.legalName}
        <input name="legalName" className={styles.input} value={form.legalName} onChange={handleChange} required />
      </label>

      <label className={styles.label}>
        {labels.relationship}
        <input name="relationship" className={styles.input} value={form.relationship} onChange={handleChange} required />
      </label>

      <label className={styles.checkLabel}>
        <input name="agreed" type="checkbox" checked={form.agreed} onChange={handleChange} />
        {labels.agree}
      </label>

      <p className={styles.disclaimer}>{labels.disclaimer}</p>

      <button className={styles.button} type="submit" disabled={!form.agreed || loading}>
        {loading ? '...' : labels.submit}
      </button>
    </form>
  );
}
